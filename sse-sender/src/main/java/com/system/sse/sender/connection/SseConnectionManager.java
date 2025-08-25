package com.system.sse.sender.connection;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseConnectionManager implements ConnectionManager {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final Long MAX_TIMEOUT = 0L; // 무제한

    @PreDestroy
    private void closeAllConnections() {
        emitters.forEach(
                (clientId, emitter) -> {
                    try {
                        emitter.complete(); // 정상 종료
                        log.debug("SSE connection closed for client: {}", clientId);
                    } catch (Exception e) {
                        log.warn(
                                "Error closing SSE connection for client {}: {}",
                                clientId,
                                e.getMessage());
                    }
                });

        emitters.clear();
    }

    /** 새로운 SSE 연결 등록 */
    @Override
    public SseEmitter connect(@Nonnull String clientId) {
        // 기존 연결이 있으면 제거
        disconnect(clientId);

        // 새 emitter 생성
        SseEmitter emitter = new SseEmitter(MAX_TIMEOUT);

        // onCompletion, onTimeout, onError 콜백 등록
        configureEmitterLifecycle(emitter, clientId);

        emitters.put(clientId, emitter);

        try {
            emitter.send(SseEmitter.event().name("connect").data("Connection successful."));
        } catch (IOException e) {
            log.error("Failed to send initial message to client {}", clientId, e);
            // 메시지 전송 실패 시 연결을 정리
            disconnect(clientId);
        }

        return emitter;
    }

    /** 클라이언트 연결 상태 확인 */
    @Override
    public boolean isConnected(String clientId) {
        return emitters.containsKey(clientId);
    }

    /** 특정 clientId의 연결 종료 및 제거 */
    @Override
    public void disconnect(String clientId) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE disconnected for client {}", clientId);
        }
    }

    /** clientId에 해당하는 emitter 등록 콜백 */
    private void configureEmitterLifecycle(SseEmitter emitter, String clientId) {
        emitter.onCompletion(
                () -> {
                    emitters.remove(clientId);
                    log.info("Emitter completed, removed client: {}", clientId);
                });
        emitter.onTimeout(
                () -> {
                    emitters.remove(clientId);
                    log.info("Emitter timed out, removed client: {}", clientId);
                });
        emitter.onError(
                error -> {
                    emitters.remove(clientId);
                    log.error("Emitter error for client {}, removed emitter", clientId, error);
                });
    }

    /** clientId에 해당하는 Emitter 조회 */
    @Override
    public Optional<SseEmitter> findByClientId(String clientId) {
        return Optional.ofNullable(emitters.get(clientId));
    }

    /** 클라이언트의 연결 ID 집합 반환 (단일 연결) */
    @Override
    public Set<String> getUserConnections(String clientId) {
        return isConnected(clientId) ? Collections.singleton(clientId) : Collections.emptySet();
    }

    /** 등록된 모든 clientId 조회 */
    @Override
    public Set<String> getAllClientIds() {
        return Collections.unmodifiableSet(emitters.keySet());
    }

    /** 활성화된 연결 수 조회 */
    @Override
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}