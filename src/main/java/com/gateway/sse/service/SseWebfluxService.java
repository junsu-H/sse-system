package com.gateway.sse.service;


import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseWebfluxService {
    private final Map<String, Sinks.Many<String>> sinkMap = new ConcurrentHashMap<>();

    public Flux<String> connect(String userId) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        sinkMap.put(userId, sink);
        return sink.asFlux()
                .doFinally(signalType -> sinkMap.remove(userId));
    }

    // 모든 구독자에게 메시지 발행
    public void send(String data) {
        sinkMap.values().forEach(sink -> sink.tryEmitNext(data));
    }
}