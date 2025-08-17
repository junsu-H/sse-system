package com.system.sse.emitter;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HeartbeatEmitter {
    private final SseEmitterService sseService;

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        sseService.sendHeartbeat();
    }
}
