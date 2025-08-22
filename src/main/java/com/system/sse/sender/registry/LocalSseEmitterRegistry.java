package com.system.sse.sender.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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
@Slf4j
@Component
public class LocalSseEmitterRegistry implements SseEmitterRegistry {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 클라이언트 ID에 해당하는 기존 Emitter 제거 후 새로 등록
     */
    @Override
    public void register(String clientId, SseEmitter emitter) {
        // 기존 emitter 정리
        remove(clientId);

        // 자동 정리 콜백 등록
        emitter.onCompletion(() -> {
            remove(clientId);
            log.info("Emitter completed, removed client: {}", clientId);
        });
        emitter.onTimeout(() -> {
            remove(clientId);
            log.info("Emitter timed out, removed client: {}", clientId);
        });
        emitter.onError(error -> {
            remove(clientId);
            log.error("Emitter error for client {}, removed emitter", clientId, error);
        });

        emitters.put(clientId, emitter);
        log.info("Registered new emitter for client: {}", clientId);
    }

    /**
     * clientId에 해당하는 Emitter 조회
     */
    @Override
    public Optional<SseEmitter> find(String clientId) {
        return Optional.ofNullable(emitters.get(clientId));
    }

    /**
     * clientId에 해당하는 Emitter 제거
     */
    @Override
    public void remove(String clientId) {
        SseEmitter removed = emitters.remove(clientId);
        if (removed != null) {
            log.info("Removed emitter for client: {}", clientId);
        }
    }

    /**
     * 등록된 모든 clientId 조회 (읽기 전용)
     */
    @Override
    public Set<String> getAllClientIds() {
        return Collections.unmodifiableSet(emitters.keySet());
    }

    /**
     * 활성화된 Emitter 수 조회
     */
    @Override
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}