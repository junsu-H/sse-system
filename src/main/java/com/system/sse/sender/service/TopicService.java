package com.system.sse.sender.service;

import com.system.sse.sender.model.SseEmitterData;

public interface TopicService {
    /**
     * 클라이언트를 특정 토픽에 구독 등록
     */
    void subscribe(String clientId, String topic);

    /**
     * 클라이언트를 특정 토픽에서 구독 해제
     */
    void unsubscribe(String clientId, String topic);

    /**
     * 특정 토픽에 속한 구독자 대상 브로드캐스트
     */
    void broadcastToTopic(String topic, SseEmitterData data);
}