package com.system.sse.application.sender.service;

import com.system.sse.application.sender.model.SseEmitterData;

/**
 * 특정 클라이언트에게 메시지 전송
 */
public interface SendService {
    void send(String clientId, SseEmitterData data);
}
