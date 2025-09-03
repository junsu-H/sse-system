package com.system.sse.application.controller;

import com.system.sse.application.service.SseVirtualThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/virtual")
@RequiredArgsConstructor
public class SseVirtualThreadController {
    private final SseVirtualThreadService sseVirtualThreadService;
    private final ExecutorService executor;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String userId) {
        boolean isVirtual = Thread.currentThread().isVirtual();
        System.out.println("[VirtualThreadCheck] Current Thread: " + Thread.currentThread() + ", isVirtual=" + isVirtual);

        // 가상 스레드에서 emitter 생성/관리
        return sseVirtualThreadService.createEmitter(userId);
    }

    @GetMapping("/publish")
    public String publish(@RequestParam(required = false) String message) {
        String msg = (message == null || message.isEmpty()) ? "Default virtual thread message" : message;
        // 모든 SSE 연결된 클라이언트에게 메시지 발송
        sseVirtualThreadService.send(msg);
        return "Published message [virtual]: " + msg;
    }

    @GetMapping(value = "/{channel}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @PathVariable String channel,
            Principal auth,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        String user = auth.getName();
        SseEmitter emitter = new SseEmitter(0L);
        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .id("init")
                        .name("connected")
                        .data("Hello " + user));
                // TODO: Redis Pub/Sub subscribe logic
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}