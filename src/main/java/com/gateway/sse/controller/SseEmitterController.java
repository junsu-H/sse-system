package com.gateway.sse.controller;

import com.gateway.sse.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/emitter")
@RequiredArgsConstructor
public class SseEmitterController {
    private final SseEmitterService sseEmitterService;

    @GetMapping("/subscribe")
    public SseEmitter subscribe(@RequestParam String userId) {
        return sseEmitterService.createEmitter(userId);
    }

    @GetMapping("/publish")
    public String publish() {
        sseEmitterService.send("Test message from SseController");
        return "Message sent to all subscribers";
    }
}
