package com.system.sse.sender.service.impl;

import com.system.sse.sender.core.SseEmitterRegistry;
import com.system.sse.sender.core.SseEventStore;
import com.system.sse.sender.model.SseEmitterData;
import com.system.sse.sender.service.SendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

/**
 * 특정 사용자에게 이벤트 전송
 */
@Service
@RequiredArgsConstructor
public class SendServiceImpl implements SendService {

    private final SseEmitterRegistry registry;
    private final SseEventStore store;

    @Override
    public void send(String clientId, SseEmitterData data) {
        SseEmitter emitter = registry.get(clientId);
        if (emitter == null) return;
        try {
            String eventId = UUID.randomUUID().toString();
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name(data.getType())
                    .data(data));
            store.store(eventId, data);
        } catch (IOException e) {
            registry.remove(clientId);
        }
    }
}
