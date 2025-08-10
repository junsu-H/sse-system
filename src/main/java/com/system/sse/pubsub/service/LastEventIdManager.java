package com.system.sse.pubsub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LastEventIdManager {
    private final RedisTemplate<String, Object> redis;
    private final Map<String, String> local = new ConcurrentHashMap<>();
    private static final String PF = "sse:last_event:";
    private static final Duration TTL = Duration.ofHours(2);

    public void update(String ch, String acc, String id) {
        String key = PF + ch + ":" + acc;
        redis.opsForValue().set(key, id, TTL);
        local.put(ch + ":" + acc, id);
    }

    public String get(String ch, String acc) {
        String c = local.get(ch + ":" + acc);
        if (c != null) return c;
        String key = PF + ch + ":" + acc;
        String v = (String) redis.opsForValue().get(key);
        if (v != null) local.put(ch + ":" + acc, v);
        return v;
    }

    public void remove(String ch, String acc) {
        String key = PF + ch + ":" + acc;
        redis.delete(key);
        local.remove(ch + ":" + acc);
    }
}