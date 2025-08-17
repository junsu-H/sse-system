package com.system.sse.emitter;

import com.system.sse.security.provider.JwtTokenParser;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    // 구독자와 만료 시각을 함께 저장하는 내부 클래스
    private static class Subscription {
        final SseEmitter emitter;
        final Instant expiry;
        Subscription(SseEmitter emitter, Instant expiry) {
            this.emitter = emitter;
            this.expiry = expiry;
        }
    }

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;
    private final AtomicLong eventIdGenerator = new AtomicLong();
    private final JwtTokenParser jwtTokenParser; // 토큰 만료 파싱용

    /**
     * 새로운 SseEmitter를 등록하고 JWT 만료 시각과 함께 저장합니다.
     */
    public SseEmitter addEmitter(String userId, SseEmitter emitter, String token, Long lastEventOffset) {
        // 1) 기존 구독 제거
        Subscription oldSub = subscriptions.remove(userId);
        if (oldSub != null) {
            oldSub.emitter.complete();
        }

        // 2) 토큰에서 만료 시각(exp) 파싱
        Claims claims = jwtTokenParser.getClaims(token);
        Instant expiry = claims.getExpiration().toInstant();

        // 3) 구독 등록
        emitter.onCompletion(() -> removeEmitter(userId));
        emitter.onTimeout(()    -> removeEmitter(userId));
        subscriptions.put(userId, new Subscription(emitter, expiry));
        log.info("SSE 연결 등록: user={} exp={}", userId, expiry);

        // 4) 재전송용 Kafka Consumer
        if (lastEventOffset != null) {
            sendMissedFromKafka(emitter, lastEventOffset);
        }

        // 5) connect 이벤트
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connection established."));
        } catch (IOException e) {
            log.error("초기 connect 이벤트 전송 실패", e);
            removeEmitter(userId);
        }

        return emitter;
    }

    /**
     * 1분마다 만료된 구독을 찾아 자동 종료합니다.
     */
    @Scheduled(fixedDelay = 60_000)
    public void expireSubscriptions() {
        Instant now = Instant.now();
        subscriptions.forEach((userId, sub) -> {
            if (now.isAfter(sub.expiry)) {
                log.info("만료된 SSE 연결 종료: user={}", userId);
                sub.emitter.complete();
                subscriptions.remove(userId);
            }
        });
    }

    /**
     * Kafka에 이벤트를 발행하고 모든 구독자에 브로드캐스트합니다.
     */
    public void broadcast(String topic, String payload) {
        long eventId = eventIdGenerator.incrementAndGet();
        kafkaTemplate.send(topic, null, String.valueOf(eventId), payload);

        subscriptions.forEach((userId, sub) -> {
            // 전송 전 만료 재확인
            if (Instant.now().isAfter(sub.expiry)) {
                log.info("만료된 연결 제거 중: user={}", userId);
                removeEmitter(userId);
                return;
            }
            try {
                sub.emitter.send(SseEmitter.event()
                        .id(String.valueOf(eventId))
                        .name("message")
                        .data(payload));
            } catch (IOException e) {
                log.warn("전송 실패, 제거: user={} eventId={}", userId, eventId, e);
                removeEmitter(userId);
            }
        });
    }

    /**
     * Kafka에서 누락 메시지를 읽어 emitter에 전송합니다.
     */
    private void sendMissedFromKafka(SseEmitter emitter, long lastOffset) {
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        String topic = "your-topic";
        consumer.subscribe(Collections.singletonList(topic));
        consumer.poll(Duration.ZERO);
        consumer.assignment().forEach(tp -> consumer.seek(tp, lastOffset + 1));

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
        records.forEach(record -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(record.key())
                        .name("message")
                        .data(record.value()));
            } catch (IOException e) {
                log.warn("Kafka 재전송 실패: offset={}", record.offset(), e);
            }
        });
        consumer.close();
    }

    /**
     * 특정 사용자의 구독을 제거합니다.
     */
    public void removeEmitter(String userId) {
        Subscription sub = subscriptions.remove(userId);
        if (sub != null) {
            sub.emitter.complete();
        }
    }

    /**
     * 하트비트 전송 (필요 시 사용)
     */
    public void sendHeartbeat() {
        subscriptions.forEach((userId, sub) -> {
            try {
                sub.emitter.send(SseEmitter.event().name("heartbeat").comment(""));
            } catch (IOException e) {
                removeEmitter(userId);
            }
        });
    }

    /**
     * 특정 사용자에게 이벤트 전송 (만료 재확인 포함)
     */
    public void sendEventToUser(String targetUserId, String eventName, Object data, long eventId) {
        log.info("=== SSE 이벤트 전송 시도 ===");
        log.info("Target UserId: {}", targetUserId);

        // 1. 정확한 매칭 시도
        Subscription exactMatch = subscriptions.get(targetUserId);
        if (exactMatch != null) {
            sendToSubscription(targetUserId, exactMatch, eventName, data, eventId);
            return;
        }

        // 2. accountId 기반 패턴 매칭 시도 (accountId:uuid 형태에서 accountId 부분만 매칭)
        log.info("정확한 매칭 실패, 패턴 매칭 시도");

        String foundUserId = null;
        Subscription patternMatch = null;

        for (Map.Entry<String, Subscription> entry : subscriptions.entrySet()) {
            String storedUserId = entry.getKey();

            // accountId:uuid 형태에서 accountId 부분 추출하여 비교
            if (storedUserId.contains(":")) {
                String accountIdPart = storedUserId.split(":")[0];
                if (accountIdPart.equals(targetUserId)) {
                    foundUserId = storedUserId;
                    patternMatch = entry.getValue();
                    log.info("패턴 매칭 성공: {} -> {}", targetUserId, storedUserId);
                    break;
                }
            }
        }

        if (patternMatch != null) {
            sendToSubscription(foundUserId, patternMatch, eventName, data, eventId);
        } else {
            log.warn("SSE 연결 없음 (정확한 매칭 및 패턴 매칭 모두 실패): user={}, eventId={}", targetUserId, eventId);
            log.info("현재 등록된 구독자들:");
            subscriptions.keySet().forEach(userId -> log.info("  - {}", userId));
        }
    }

    /**
     * 실제 구독자에게 메시지 전송
     */
    private void sendToSubscription(String userId, Subscription sub, String eventName, Object data, long eventId) {
        if (Instant.now().isAfter(sub.expiry)) {
            log.info("만료된 연결 제거 중: user={}", userId);
            removeEmitter(userId);
            return;
        }

        try {
            sub.emitter.send(SseEmitter.event()
                    .id(String.valueOf(eventId))
                    .name(eventName)
                    .data(data));
            log.info("✅ SSE 이벤트 전송 성공: user={}, eventId={}, eventName={}", userId, eventId, eventName);
        } catch (IOException e) {
            log.error("❌ SSE 이벤트 전송 실패: user={}, eventId={}, eventName={}", userId, eventId, eventName, e);
            removeEmitter(userId);
        }
    }

    public void logCurrentSubscriptions() {
        log.info("=== 현재 SSE 구독자 목록 ===");
        log.info("총 구독자 수: {}", subscriptions.size());

        if (subscriptions.isEmpty()) {
            log.warn("구독자가 없습니다!");
        } else {
            subscriptions.forEach((userId, sub) -> {
                log.info("구독자 - UserId: {}, 만료시간: {}, 연결상태: Active",
                        userId, sub.expiry);
            });
        }
    }

    /**
     * 브로드캐스트 테스트 (모든 구독자에게 전송)
     */
    public void testBroadcast(String message) {
        log.info("=== 브로드캐스트 테스트 시작 ===");
        log.info("메시지: {}", message);

        subscriptions.forEach((userId, sub) -> {
            try {
                sub.emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("test_broadcast")
                        .data(message));
                log.info("브로드캐스트 전송 성공: userId={}", userId);
            } catch (IOException e) {
                log.error("브로드캐스트 전송 실패: userId={}", userId, e);
                removeEmitter(userId);
            }
        });
    }

    public void sendHeartbeatStyleTest(String userId, String message) {
        log.info("=== 하트비트 방식 테스트 메시지 전송 ===");
        log.info("UserId: {}, Message: {}", userId, message);

        for (Map.Entry<String, Subscription> entry : subscriptions.entrySet()) {
            String storedUserId = entry.getKey();

            if (storedUserId.contains(":") && storedUserId.split(":")[0].equals(userId)) {
                Subscription sub = entry.getValue();

                try {
                    // 하트비트와 동일한 방식 (comment 사용)
                    sub.emitter.send(SseEmitter.event()
                            .name("test_message")
                            .comment(message));  // comment로 전송 (data 대신)

                    log.info("✅ 하트비트 방식 테스트 전송 성공: user={}", storedUserId);
                    break;

                } catch (IOException e) {
                    log.error("❌ 하트비트 방식 테스트 전송 실패: user={}", storedUserId, e);
                    removeEmitter(storedUserId);
                }
            }
        }
    }

    /**
     * 단순 문자열 데이터로 테스트
     */
    public void sendSimpleStringData(String userId, String message) {
        log.info("=== 단순 문자열 데이터 테스트 ===");
        log.info("UserId: {}, Message: {}", userId, message);

        for (Map.Entry<String, Subscription> entry : subscriptions.entrySet()) {
            String storedUserId = entry.getKey();

            if (storedUserId.contains(":") && storedUserId.split(":")[0].equals(userId)) {
                Subscription sub = entry.getValue();

                try {
                    // 단순 문자열로 전송 (JSON 직렬화 없음)
                    sub.emitter.send(SseEmitter.event()
                            .name("simple_string")
                            .data(message));  // 단순 문자열

                    log.info("✅ 단순 문자열 테스트 전송 성공: user={}", storedUserId);
                    break;

                } catch (IOException e) {
                    log.error("❌ 단순 문자열 테스트 전송 실패: user={}", storedUserId, e);
                    removeEmitter(storedUserId);
                }
            }
        }
    }
}
