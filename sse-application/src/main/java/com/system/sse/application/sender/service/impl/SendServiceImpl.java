package com.system.sse.application.sender.service.impl;

import com.system.sse.application.sender.registry.LocalSseEmitterRegistry;
import com.system.sse.application.sender.store.LocalSseEventStore;
import com.system.sse.application.sender.model.SseEmitterData;
import com.system.sse.application.sender.service.SendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

/**
 * 특정 사용자에게 이벤트 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendServiceImpl implements SendService {

    private final LocalSseEmitterRegistry registry;
    private final LocalSseEventStore store;

    @Override
    public void send(String clientId, SseEmitterData data) {
        registry.find(clientId).ifPresentOrElse(emitter -> {
            String eventId = UUID.randomUUID().toString();
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .id(eventId)
                    .name(data.getType())
                    .data(data);
            try {
                emitter.send(event);
                store.store(eventId, data);
                log.debug("Sent event {} to client {}", eventId, clientId);
            } catch (IOException e) {
                log.warn("Error sending event to client {}: {}", clientId, e.getMessage());
                cleanup(clientId, emitter);
            }
        }, () -> log.warn("No active SSE connection for client {}", clientId));
    }

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
