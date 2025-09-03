package com.system.sse.application.sender.controller;

import com.system.sse.application.sender.service.*;
import com.system.sse.application.sender.model.SseEmitterData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * SSE Controller
 */
@Slf4j
@RestController
@RequestMapping("/sse/v2")
@RequiredArgsConstructor
public class SseEmitterControllerV2 {
    private final ConnectionService connectionService;
    private final SendService sendService;
    private final BroadcastService broadcastService;
    private final ReplayService replayService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final TopicService topicService;

    /**
     * SSE 연결 요청
     * @param clientId 클라이언트 식별자
     * @param lastEventId Last-Event-ID 헤더(Optional)
     */
    @GetMapping(path = "/subscribe/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @PathVariable String clientId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    ) throws IOException {
        log.info("Subscribe request from client {} (Last-Event-ID={})", clientId, lastEventId);
        return connectionService.connect(clientId, lastEventId);
    }

    /**
     * 명시적 구독 해제
     */
    @DeleteMapping("/unsubscribe/{clientId}")
    public void unsubscribe(@PathVariable String clientId) {
        log.info("Unsubscribe request from client {}", clientId);
        connectionService.disconnect(clientId);
    }

    /**
     * 전체 브로드캐스트
     */
    @PostMapping("/broadcast")
    public void broadcast(@RequestBody SseEmitterData data) {
        log.info("Broadcasting event type {}", data.getType());
        broadcastService.broadcast(data);
    }

    /**
     * 특정 클라이언트에게 전송
     */
    @PostMapping("/send/{clientId}")
    public void sendToClient(
            @PathVariable String clientId,
            @RequestBody SseEmitterData data
    ) {
        log.info("Sending event to client {} type {}", clientId, data.getType());
        sendService.send(clientId, data);
    }

    /**
     * 누락된 이벤트 재전송
     */
    @PostMapping("/replay/{clientId}")
    public void replayMissed(
            @PathVariable String clientId,
            @RequestParam("lastEventId") String lastEventId
    ) {
        log.info("Replaying missed events to client {} since {}", clientId, lastEventId);
        replayService.replayMissed(clientId, lastEventId);
    }

    /**
     * 시간 범위로 이벤트 재전송
     */
    @PostMapping("/replay/{clientId}/range")
    public void replayRange(
            @PathVariable String clientId,
            @RequestParam("from") Instant from,
            @RequestParam("to") Instant to
    ) {
        log.info("Replaying events to client {} from {} to {}", clientId, from, to);
        replayService.replayRange(clientId, from, to);
    }

    /**
     * 토픽 구독 등록
     */
    @PostMapping("/topic/subscribe")
    public void subscribeTopic(
            @RequestParam String clientId,
            @RequestParam String topic
    ) {
        log.info("Client {} subscribes to topic {}", clientId, topic);
        topicService.subscribe(clientId, topic);
    }

    /**
     * 토픽 구독 해제
     */
    @PostMapping("/topic/unsubscribe")
    public void unsubscribeTopic(
            @RequestParam String clientId,
            @RequestParam String topic
    ) {
        log.info("Client {} unsubscribes from topic {}", clientId, topic);
        topicService.unsubscribe(clientId, topic);
    }

    /**
     * 토픽 브로드캐스트
     */
    @PostMapping("/topic/broadcast")
    public void broadcastTopic(
            @RequestParam String topic,
            @RequestBody SseEmitterData data
    ) {
        log.info("Broadcasting to topic {} event type {}", topic, data.getType());
        topicService.broadcastToTopic(topic, data);
    }

    /**
     * 전체 구독자 조회
     */
    @GetMapping("/clients")
    public Set<String> listAllClients() {
        return subscriptionQueryService.listAllClients();
    }

    /**
     * 토픽별 구독자 조회
     */
    @GetMapping("/clients/topic/{topic}")
    public Set<String> listClientsByTopic(@PathVariable String topic) {
        return subscriptionQueryService.listClientsByTopic(topic);
    }
}
