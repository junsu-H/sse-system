package com.system.sse.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.sse.service.SseEmitterService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessageConsumer {

    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "sse-notifications", groupId = "sse-consumer-group")
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            log.info("=== Kafka 메시지 수신 ===");
            log.info("Topic: {}, Partition: {}, Offset: {}", record.topic(), record.partition(), record.offset());
            log.info("Key: {}", record.key());
            log.info("Value: {}", record.value());

            // Kafka 메시지를 EventMessage 객체로 역직렬화
            EventMessage event = objectMapper.readValue(record.value(), EventMessage.class);

            String userId = String.valueOf(event.getAccountId());
            String eventName = event.getEventName();
            Object data = event.getData();

            log.info("=== 파싱된 이벤트 정보 ===");
            log.info("UserId: {}", userId);
            log.info("EventName: {}", eventName);
            log.info("Data: {}", data);
            log.info("Timestamp: {}", event.getTimestamp());

            // ✅ 현재 구독자 목록 확인 (디버깅용)
            log.info("=== 현재 SSE 구독자 확인 ===");
            sseEmitterService.logCurrentSubscriptions(); // 이 메서드를 추가해야 함

            // SseEmitterService를 사용해 클라이언트에 이벤트 전송
            long eventId = System.currentTimeMillis();
            sseEmitterService.sendEventToUser(userId, eventName, data, eventId);
            log.info("=== SSE 전송 시도 완료 ===");
            log.info("Target UserId: {}, EventId: {}", userId, eventId);

        } catch (Exception e) {
            log.error("Kafka Consumer: Failed to process record {}: {}", record, e.getMessage(), e);
        }
    }

    @Data
    @AllArgsConstructor
    static class EventMessage {
        private String id;
        private Long accountId;
        private String eventName;
        private Object data;
        private long timestamp;
    }
}
