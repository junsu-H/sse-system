package com.system.sse.application.sender.helper;

import com.system.sse.application.sender.model.SseEmitterData;
import com.system.sse.application.sender.registry.SseEmitterRegistry;
import com.system.sse.application.sender.store.LocalSseEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventDispatcher {

    private final SseEmitterRegistry registry;
    private final LocalSseEventStore store;

    /**
     * 특정 클라이언트에게 이벤트 전송
     */
    public void dispatch(String clientId, SseEmitter.SseEventBuilder eventBuilder) {
        registry.find(clientId).ifPresentOrElse(emitter -> {
            String eventId = UUID.randomUUID().toString();
            try {
                emitter.send(eventBuilder.id(eventId).build());
                store.store(eventId, (SseEmitterData) eventBuilder.build());
                log.debug("Dispatched event {} to client {}", eventId, clientId);
            } catch (IOException e) {
                log.warn("Failed to dispatch event {} to client {}: {}", eventId, clientId, e.getMessage());
                cleanup(clientId, emitter);
            }
        }, () -> log.warn("No active emitter for client {}", clientId));
    }

    /**
     * 모든 클라이언트에게 이벤트 전송
     */
    public void dispatchAll(SseEmitter.SseEventBuilder eventBuilder) {
        registry.getAllClientIds().forEach(clientId ->
                dispatch(clientId, eventBuilder)
        );
    }

    private void cleanup(String clientId, SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignore) {
        }
        registry.remove(clientId);
        log.info("Cleaned up emitter for client {}", clientId);
    }
}