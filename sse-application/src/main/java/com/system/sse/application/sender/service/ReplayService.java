package com.system.sse.application.sender.service;

import java.time.Instant;

public interface ReplayService {
    /**
     * 특정 클라이언트에 대해 lastEventId 이후의 누락 이벤트 전송
     */
    void replayMissed(String clientId, String lastEventId);

    /**
     * 특정 클라이언트에 대해 타임스탬프 범위 내 이벤트 재전송
     */
    void replayRange(String clientId, Instant from, Instant to);
}