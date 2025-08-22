package com.system.sse.sender.registry;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 연결된 SSE Emitter를 관리하는 레지스트리 (스레드 안전)
 */
public interface SseEmitterRegistry {
    void register(String clientId, SseEmitter emitter);

    Optional<SseEmitter> find(String clientId);

    void remove(String clientId);

    Set<String> getAllClientIds();

    int getActiveConnectionCount();
}
