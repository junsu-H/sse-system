package com.system.sse.sender.controller;

import com.system.sse.sender.model.SseEmitterData;
import com.system.sse.sender.service.BroadcastService;
import com.system.sse.sender.service.ConnectionService;
import com.system.sse.sender.service.SendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * SSE Controller
 */
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseEmitterController {

    private final ConnectionService connectionService;
    private final SendService sendService;
    private final BroadcastService broadcastService;

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin(origins = "*")
    public SseEmitter connect(@RequestParam String clientId,
                              @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) throws IOException {
        return connectionService.connect(clientId, lastEventId);
    }

    @PostMapping("/send/{clientId}")
    public void send(@PathVariable String clientId, @RequestBody SseEmitterData data) {
        sendService.send(clientId, data);
    }

    @PostMapping("/broadcast")
    public void broadcast(@RequestBody SseEmitterData data) {
        broadcastService.broadcast(data);
    }
}
