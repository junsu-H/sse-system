package com.system.sse.sender.store;

import com.system.sse.sender.model.SseEmitterData;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 메모리 기반 이벤트 스토어 (Last-Event-ID 재전송 지원)
 * - 단일 노드 환경용 (멀티 노드 환경에서는 Redis/Kafka 필요)
 */
@Component
public class LocalSseEventStore {

    private final List<StoredEvent> events = new CopyOnWriteArrayList<>();
    private final int maxSize = 1000; // 버퍼 크기 제한

    public void store(String eventId, SseEmitterData data) {
        if (events.size() >= maxSize) {
            events.removeFirst();
        }
        events.add(new StoredEvent(eventId, data, Instant.now()));
    }

    public List<SseEmitterData> fetchSince(String lastEventId) {
        if (lastEventId == null) return List.of();
        return events.stream()
                .filter(e -> e.id.compareTo(lastEventId) > 0)
                .map(e -> e.data)
                .collect(Collectors.toList());
    }

    public List<SseEmitterData> fetchBetween(Instant from, Instant to) {
        if (from == null || to == null || from.isAfter(to)) {
            return List.of();
        }
        return events.stream()
                .filter(e -> !e.timestamp.isBefore(from) && !e.timestamp.isAfter(to))
                .map(e -> e.data)
                .collect(Collectors.toList());
    }

    private record StoredEvent(String id, SseEmitterData data, Instant  timestamp) {}
}
