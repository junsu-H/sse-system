package com.system.sse.sender.service;

import com.system.sse.sender.model.SseEmitterData;

/**
 * 전체 클라이언트에게 메시지 전송
 */
public interface BroadcastService {
    void broadcast(SseEmitterData data);
}
