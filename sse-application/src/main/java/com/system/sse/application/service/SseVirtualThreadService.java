package com.system.sse.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseVirtualThreadService {
    // userId별 SseEmitter 관리
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    // 가상 스레드 실행자
    private final Executor exec;

    // SSE 커넥션 등록 및 관리
    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitterMap.put(userId, emitter);

        emitter.onCompletion(() -> emitterMap.remove(userId));
        emitter.onTimeout(() -> emitterMap.remove(userId));

        log.info("Registered SSE virtual emitter for user: {}", userId);
        return emitter;
    }

    // 가상스레드로 각 클라이언트에 메시지 송신
    public void send(String data) {
        for (Map.Entry<String, SseEmitter> entry : emitterMap.entrySet()) {
            String userId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            // 가상스레드 활용: blocking send임에도 수천~만 커넥션에도 오버헤드 적음
            exec.execute(() -> {
                try {
                    emitter.send(SseEmitter.event().data(data));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                    emitterMap.remove(userId);
                    log.warn("Failed to send SSE [virtual] to {}: {}", userId, e.getMessage());
                }
            });
        }
    }
}