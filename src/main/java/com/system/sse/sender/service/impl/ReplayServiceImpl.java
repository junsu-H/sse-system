package com.system.sse.sender.service.impl;

import com.system.sse.sender.model.SseEmitterData;
import com.system.sse.sender.registry.LocalSseEmitterRegistry;
import com.system.sse.sender.store.LocalSseEventStore;
import com.system.sse.sender.service.ReplayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplayServiceImpl implements ReplayService {

    private final LocalSseEmitterRegistry registry;
    private final LocalSseEventStore store;


    /**
     * lastEventId 이후의 누락된 이벤트를 re-send
     */
    @Override
    public void replayMissed(String clientId, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            log.debug("No lastEventId provided for client {}", clientId);
            return;
        }
        registry.find(clientId).ifPresentOrElse(emitter -> {
            List<SseEmitterData> events = store.fetchSince(lastEventId);
            if (events.isEmpty()) {
                log.debug("No missed events to replay for client {}", clientId);
                return;
            }
            log.info("Replaying {} missed events to client {}", events.size(), clientId);
            events.forEach(data -> sendEvent(emitter, clientId, data));
        }, () -> log.warn("Cannot replay missed events; no active connection for client {}", clientId));
    }

    /**
     * from ~ to 타임스탬프 범위 내 이벤트를 re-send
     */
    @Override
    public void replayRange(String clientId, Instant from, Instant to) {
        if (from == null || to == null || from.isAfter(to)) {
            log.warn("Invalid replay range for client {}: from={} to={}", clientId, from, to);
            return;
        }
        registry.find(clientId).ifPresentOrElse(emitter -> {
            List<SseEmitterData> events = store.fetchBetween(from, to);
            if (events.isEmpty()) {
                log.debug("No events in range to replay for client {}", clientId);
                return;
            }
            log.info("Replaying {} events to client {} between {} and {}", events.size(), clientId, from, to);
            events.forEach(data -> sendEvent(emitter, clientId, data));
        }, () -> log.warn("Cannot replay events; no active connection for client {}", clientId));
    }

    /**
     * 개별 이벤트 전송 및 실패 시 정리
     */
    private void sendEvent(SseEmitter emitter, String clientId, SseEmitterData data) {
        String eventId = UUID.randomUUID().toString(); // TODO: epoch + UUID 조합으로 고유 ID 생성
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name(data.getType())
                    .data(data));
            log.debug("Replayed event {} to client {}", eventId, clientId);
        } catch (IOException e) {
            log.error("Failed to replay event {} to client {}: {}", eventId, clientId, e.getMessage());
            cleanup(clientId, emitter);
            // 중간 오류 시 더 이상의 재전송 중단
            throw new RuntimeException(e);
        }
    }

    /**
     * 연결 실패 시 Emitter 종료 및 레지스트리에서 제거
     */
    private void cleanup(String clientId, SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignore) {
            // ignore
        }
        registry.remove(clientId);
        log.info("Cleaned up SSE connection for client {}", clientId);
    }
}
