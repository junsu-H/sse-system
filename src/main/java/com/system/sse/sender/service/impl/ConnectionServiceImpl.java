package com.system.sse.sender.service.impl;

import com.system.sse.sender.core.SseEmitterRegistry;
import com.system.sse.sender.core.SseEventStore;
import com.system.sse.sender.service.ConnectionService;
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

    private final SseEmitterRegistry registry;
    private final SseEventStore store;

    @Override
    public SseEmitter connect(String clientId, String lastEventId) throws IOException {
        // 1. 새로운 emitter 생성 (타임아웃 무제한)
        SseEmitter emitter = new SseEmitter(0L);

        // 2. Registry에 등록 (자동 lifecycle 관리 포함)
        registry.add(clientId, emitter);

        // 3. 초기 연결 이벤트 전송
        sendInitEvent(emitter);

        // 4. Last-Event-ID 처리
        resendMissedEvents(clientId, emitter, lastEventId);

        return emitter;
    }

    /**
     * 초기 연결 이벤트 전송
     */
    private void sendInitEvent(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .id(UUID.randomUUID().toString())
                    .data("connected"));
        } catch (IOException e) {
            // 초기 전송 실패 시 emitter 종료
            emitter.completeWithError(e);
        }
    }

    /**
     * Last-Event-ID 이후 누락된 이벤트 재전송
     */
    private void resendMissedEvents(String clientId, SseEmitter emitter, String lastEventId) {
        if (lastEventId == null) return;

        store.fetchSince(lastEventId).forEach(data -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(UUID.randomUUID().toString())
                        .name(data.getType())
                        .data(data));
            } catch (IOException e) {
                log.warn("Failed to resend event {} to client {}: {}", data.getResourceId(), clientId, e.getMessage());
                emitter.completeWithError(e);   // 연결 종료
                registry.remove(clientId);      // registry에서 제거
            }
        });
    }
}
