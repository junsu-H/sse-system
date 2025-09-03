package com.system.sse.application.sender.service.impl;

import com.system.sse.application.sender.registry.LocalSseEmitterRegistry;
import com.system.sse.application.sender.store.LocalSseEventStore;
import com.system.sse.application.sender.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

/**
 * SSE 연결 관리 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionServiceImpl implements ConnectionService {

    private final LocalSseEmitterRegistry registry;
    private final LocalSseEventStore store;

    @Override
    public SseEmitter connect(String clientId, String lastEventId){
        // 1. 새로운 emitter 생성 (타임아웃 무제한)
        SseEmitter emitter = new SseEmitter(0L);

        // 2. Registry에 등록 (자동 lifecycle 관리 포함)
        registry.register(clientId, emitter);

        // 3. 초기 연결 이벤트 전송
        sendInitEvent(clientId, emitter);

        // 4. Last-Event-ID 처리
        resendMissedEvents(clientId, emitter, lastEventId);

        return emitter;
    }

    @Override
    public void disconnect(String clientId) {
        registry.find(clientId).ifPresent(emitter -> {
            try {
                emitter.complete();
                log.info("Client {} disconnected successfully", clientId);
            } catch (Exception e) {
                log.warn("Error during disconnect for client {}: {}", clientId, e.getMessage());
            }
        });
        registry.remove(clientId);
    }


    /**
     * 초기 연결 이벤트 전송
     */
    private void sendInitEvent(String clientId, SseEmitter emitter) {
        try {
            String eventId = UUID.randomUUID().toString();
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .id(eventId)
                    .data("connected"));
            log.debug("Initial event sent to client: {}", clientId);
        } catch (IOException e) {
            log.error("Failed to send initial event to client {}: {}", clientId, e.getMessage());
            emitter.completeWithError(e);
            registry.remove(clientId);
        }
    }

    /**
     * Last-Event-ID 이후 누락된 이벤트 재전송
     */
    private void resendMissedEvents(String clientId, SseEmitter emitter, String lastEventId) {
        if (lastEventId == null || lastEventId.trim().isEmpty()) {
            return;
        }

        try {
            store.fetchSince(lastEventId).forEach(data -> {
                try {
                    String eventId = UUID.randomUUID().toString();
                    emitter.send(SseEmitter.event()
                            .id(eventId)
                            .name(data.getType())
                            .data(data));
                } catch (IOException e) {
                    log.warn("Failed to resend event to client {}: {}", clientId, e.getMessage());
                    throw new RuntimeException(e); // forEach 중단을 위해 RuntimeException으로 변환
                }
            });
            log.info("Resent missed events to client {} from lastEventId: {}", clientId, lastEventId);
        } catch (Exception e) {
            log.error("Error during event resend for client {}: {}", clientId, e.getMessage());
            emitter.completeWithError(e);
            registry.remove(clientId);
        }
    }
}
