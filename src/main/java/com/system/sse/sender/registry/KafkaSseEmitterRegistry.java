package com.system.sse.sender.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Profile("kafka")
@Component
@RequiredArgsConstructor
public class KafkaSseEmitterRegistry implements SseEmitterRegistry {
    private final KafkaRegistryPublisher publisher;
    private final Map<String, SseEmitter> localCache = new ConcurrentHashMap<>();

    private static final String REGISTRY_TOPIC = "client-registry";

    /**
     * 클라이언트 등록: 로컬 캐시에 emitter 저장 후 Kafka에 REGISTER 이벤트 퍼블리시
     */
    @Override
    public void register(String clientId, SseEmitter emitter) {
        remove(clientId);
        // 자동 정리 콜백
        emitter.onCompletion(() -> remove(clientId));
        emitter.onTimeout(() -> remove(clientId));
        emitter.onError(error -> remove(clientId));

        localCache.put(clientId, emitter);
        publisher.publishRegister(clientId);
        log.info("Registered and published REGISTER for client: {}", clientId);
    }

    /**
     * 클라이언트 조회: 로컬 캐시에서 emitter 반환
     */
    @Override
    public Optional<SseEmitter> find(String clientId) {
        return Optional.ofNullable(localCache.get(clientId));
    }

    /**
     * 클라이언트 제거: 로컬 캐시에서 제거 후 Kafka에 REMOVE 이벤트 퍼블리시
     */
    @Override
    public void remove(String clientId) {
        SseEmitter removed = localCache.remove(clientId);
        publisher.publishRemove(clientId);
        log.info("Published REMOVE for client: {}", clientId);
        if (removed != null) {
            log.info("Removed emitter for client: {}", clientId);
        }
    }

    /**
     * 모든 클라이언트 ID 조회 (읽기 전용)
     */
    @Override
    public Set<String> getAllClientIds() {
        return Collections.unmodifiableSet(localCache.keySet());
    }

    /**
     * 활성화된 연결 수 조회
     */
    @Override
    public int getActiveConnectionCount() {
        return localCache.size();
    }
}