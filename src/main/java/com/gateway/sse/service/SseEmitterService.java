package com.gateway.sse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        return emitter;
    }

    // userId별로 메시지 분기하려면 메시지에 userId 포함 필요
    public void send(String data) {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitters.remove(userId);
            }
        });
    }
}
