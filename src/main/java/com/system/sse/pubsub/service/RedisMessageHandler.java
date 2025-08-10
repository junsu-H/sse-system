package com.system.sse.pubsub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.sse.pubsub.entity.SseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageHandler implements MessageListener {

//    private final SseEmitterManager emitterManager;
    private final EventCacheService cacheService;
    private final LastEventIdManager lastEventIdManager;
    private final ObjectMapper objectMapper;
    private final ExecutorService vtExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        vtExecutor.submit(() -> {
            try {
                String topic = new String(message.getChannel());
                String channel = topic.substring(topic.indexOf(':') + 1);
                SseEvent event = objectMapper.readValue(message.getBody(), SseEvent.class);

                cacheService.cacheEvent(event);
//                emitterManager.broadcastToChannel(channel, event);
                lastEventIdManager.update(channel, event.getAccountId(), event.getLastEventId());
            } catch (Exception ex) {
                log.error("Error processing Redis message", ex);
            }
        });
    }
}
