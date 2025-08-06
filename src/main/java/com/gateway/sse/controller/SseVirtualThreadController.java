package com.gateway.sse.controller;

import com.gateway.sse.service.SseVirtualThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/virtual")
@RequiredArgsConstructor
public class SseVirtualThreadController {
    private final SseVirtualThreadService sseVirtualThreadService;

    @GetMapping("/subscribe")
    public SseEmitter subscribe(@RequestParam String userId) {
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
}