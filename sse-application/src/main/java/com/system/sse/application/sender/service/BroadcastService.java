package com.system.sse.application.sender.service;

import com.system.sse.application.sender.model.SseEmitterData;

/**
 * 전체 클라이언트에게 메시지 전송
 */
public interface BroadcastService {
    void broadcast(SseEmitterData data);
}
