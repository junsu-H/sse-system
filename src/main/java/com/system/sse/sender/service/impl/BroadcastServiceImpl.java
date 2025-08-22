package com.system.sse.sender.service.impl;

import com.system.sse.sender.registry.LocalSseEmitterRegistry;
import com.system.sse.sender.store.LocalSseEventStore;
import com.system.sse.sender.model.SseEmitterData;
import com.system.sse.sender.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

/**
 * 전체 클라이언트에게 이벤트 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastServiceImpl implements BroadcastService {

    private final LocalSseEmitterRegistry registry;
    private final LocalSseEventStore store;

    @Override
    public void broadcast(SseEmitterData data) {
        registry.getAllClientIds().forEach(clientId -> registry.find(clientId).ifPresent(emitter -> {
            try {
                String eventId = UUID.randomUUID().toString();
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .id(eventId)
                        .name(data.getType())
                        .data(data);
                emitter.send(event);
                store.store(eventId, data);
            } catch (IOException e) {
                log.warn("Failed to send broadcast to client {}: {}", clientId, e.getMessage());
                registry.remove(clientId);
            }
        }));
    }
}
