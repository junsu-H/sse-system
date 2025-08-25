package com.system.sse.sender.connection;

import java.util.Optional;
import java.util.Set;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ConnectionManager {
    /**
     * 새로운 SSE 연결을 생성하고, clientId를 반환합니다.
     *
     * @param clientId 클라이언트 식별자
     * @return clientId
     */
    SseEmitter connect(String clientId);

    /**
     * clientId에 해당하는 연결이 활성 상태인지 여부를 반환합니다.
     *
     * @param clientId 클라이언트 식별자
     * @return true: 연결 중, false: 연결 없음
     */
    boolean isConnected(String clientId);

    /**
     * clientId에 해당하는 SSE 연결을 종료하고 제거합니다.
     *
     * @param clientId 클라이언트 식별자
     */
    void disconnect(String clientId);

    /**
     * clientId에 해당하는 모든 연결 ID 집합을 반환합니다.
     *
     * @param clientId 클라이언트 식별자
     * @return connectionId 집합
     */
    Set<String> getUserConnections(String clientId);

    /**
     * clientId에 해당하는 SseEmitter를 조회합니다.
     *
     * @param clientId 클라이언트 식별자
     * @return Optional<SseEmitter>
     */
    Optional<SseEmitter> findByClientId(String clientId);

    /**
     * 모든 등록된 clientId 집합을 반환합니다.
     *
     * @return clientId 집합
     */
    Set<String> getAllClientIds();

    /**
     * 활성화된 SSE 연결 수를 반환합니다.
     *
     * @return 연결 수
     */
    int getActiveConnectionCount();
}