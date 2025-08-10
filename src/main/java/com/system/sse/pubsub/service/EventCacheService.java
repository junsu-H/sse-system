package com.system.sse.pubsub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.sse.pubsub.entity.SseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventCacheService {
    private final RedisTemplate<String, Object> redis;
    private final ObjectMapper om;
    private static final String PF = "sse:events:";
    private static final Duration TTL = Duration.ofHours(2);
    private static final int MAX = 1000;

    @Async
    public void cacheEvent(SseEvent e) {
        try {
            String key = PF + e.getChannel();
            String j = om.writeValueAsString(e);
            redis.opsForZSet().add(key, j, e.getTimestamp());
            long total = redis.opsForZSet().zCard(key);
            if (total > MAX) redis.opsForZSet().removeRange(key, 0, total - MAX - 1);
            redis.expire(key, TTL);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public List<SseEvent> getEventsAfter(String ch, String last) {
        String key = PF + ch;
        long lt = Long.parseLong(Arrays.toString(last.split("_")));

        var set = redis.opsForZSet().rangeByScore(key, lt + 1, Double.MAX_VALUE);
        if (set == null) return List.of();
        return set.stream().map(s -> {
            try {
                return om.readValue(s.toString(), SseEvent.class);
            } catch (Exception ex) {
                return null;
            }
        }).filter(Objects::nonNull).sorted(Comparator.comparingLong(SseEvent::getTimestamp)).collect(Collectors.toList());
    }
}