package com.system.sse.sender.core;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 연결된 SSE Emitter를 관리하는 레지스트리 (스레드 안전)
 */

@Component
public class SseEmitterRegistry {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 새로운 Emitter 등록
     * 동일 clientId가 이미 존재하면 기존 emitter를 제거 후 새로 등록
     */
    public void add(String clientId, SseEmitter emitter) {
        // 기존 emitter 정리
        remove(clientId);

        // 자동 정리 콜백 등록
        emitter.onCompletion(() -> remove(clientId));
        emitter.onTimeout(() -> remove(clientId));
        emitter.onError(e -> remove(clientId));

        emitters.put(clientId, emitter);
    }

    /**
     * 특정 clientId의 emitter 조회
     */
    public SseEmitter get(String clientId) {
        return emitters.get(clientId);
    }

    /**
     * 특정 clientId emitter 제거
     */
    public void remove(String clientId) {
        emitters.remove(clientId);
    }

    /**
     * 모든 emitter 조회 (읽기 전용 뷰)
     */
    public Map<String, SseEmitter> findAll() {
        return Collections.unmodifiableMap(emitters);
    }
}