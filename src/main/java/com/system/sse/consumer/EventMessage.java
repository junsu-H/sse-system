package com.system.sse.consumer;

import com.system.sse.emitter.EmitterData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka Consumer → SSE 전송 시 사용하는 메시지 래퍼
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage {
    private String id;             // Kafka 이벤트 ID
    private Long accountId;        // 대상 사용자 계정 ID
    private String eventName;      // 이벤트명 (SSE event 타입)
    private EmitterData data;      // 실제 페이로드 (EmitterData 사용)
    private long timestamp;        // Kafka 발행 시간
}
