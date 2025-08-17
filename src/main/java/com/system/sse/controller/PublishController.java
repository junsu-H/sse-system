package com.system.sse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.sse.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class PublishController {

    private final SseEmitterService sseEmitterService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Kafka를 통한 메시지 발행 (MSA Server 역할 시뮬레이션)
     * MSA Server → Kafka → Consumer → SSE → Client 플로우
     */
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishToKafka(@RequestBody KafkaPublishRequest request) {

        log.info("=== Kafka로 메시지 발행 (MSA Server 시뮬레이션) ===");
        log.info("AccountId: {}", request.getAccountId());
        log.info("EventName: {}", request.getEventName());
        log.info("Data: {}", request.getData());

        try {
            // EventMessage 객체 생성 (KafkaMessageConsumer에서 받을 형태)
            EventMessage eventMessage = new EventMessage(
                    "event-" + System.currentTimeMillis(), // id
                    request.getAccountId(),                  // accountId
                    request.getEventName(),                  // eventName
                    request.getData(),                       // data
                    System.currentTimeMillis()               // timestamp
            );

            // JSON으로 직렬화
            String jsonMessage = objectMapper.writeValueAsString(eventMessage);

            // Kafka "sse-notifications" 토픽에 발행
            kafkaTemplate.send("sse-notifications", String.valueOf(request.getAccountId()), jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka 메시지 발행 성공: topic=sse-notifications, key={}, offset={}",
                                    request.getAccountId(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Kafka 메시지 발행 실패: {}", ex.getMessage(), ex);
                        }
                    });

            log.info("Kafka 메시지 발행 요청 완료 - Consumer가 처리하여 SSE로 전송할 예정");

            // 성공 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "메시지가 Kafka로 발행되었습니다. KafkaMessageConsumer가 처리하여 SSE로 전송합니다.");
            response.put("method", "kafka-publish");
            response.put("topic", "sse-notifications");
            response.put("accountId", request.getAccountId());
            response.put("eventName", request.getEventName());
            response.put("publishedAt", Instant.now().toString());
            response.put("eventMessage", eventMessage);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Kafka 메시지 발행 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Kafka 메시지 발행 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    // EventMessage 클래스 (KafkaMessageConsumer와 동일한 구조)
    public static class EventMessage {
        private String id;
        private Long accountId;
        private String eventName;
        private Object data;
        private long timestamp;

        public EventMessage() {}

        public EventMessage(String id, Long accountId, String eventName, Object data, long timestamp) {
            this.id = id;
            this.accountId = accountId;
            this.eventName = eventName;
            this.data = data;
            this.timestamp = timestamp;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    // 직접 SSE 전송용 요청 DTO
    public static class MsaMessageRequest {
        private String type; // "broadcast" 또는 "user"
        private String target; // 사용자 ID (user 타입인 경우)
        private String eventName; // SSE 이벤트명
        private Object data; // 실제 메시지 데이터
        private Map<String, Object> metadata; // 추가 메타데이터

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    // Kafka 발행용 요청 DTO
    public static class KafkaPublishRequest {
        private Long accountId; // 사용자 계정 ID
        private String eventName; // 이벤트명
        private Object data; // 메시지 데이터

        // Getters and Setters
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

}
