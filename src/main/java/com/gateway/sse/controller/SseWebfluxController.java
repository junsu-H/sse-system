package com.gateway.sse.controller;


import com.gateway.sse.service.SseWebfluxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class SseWebfluxController {
    private final SseWebfluxService sseWebfluxService;

    @GetMapping("/flux/subscribe")
    public Flux<ServerSentEvent<String>> subscribe(@RequestParam String userId) {
        return sseWebfluxService.connect(userId)
                .map(data -> ServerSentEvent.builder(data).build());
    }

    @GetMapping("/flux/publish")
    public String publish(@RequestParam(required = false) String message) {
        String sendMsg = (message == null || message.isEmpty()) ? "Default event message" : message;
        sseWebfluxService.send(sendMsg);
        return "Published message: " + sendMsg;
    }
}