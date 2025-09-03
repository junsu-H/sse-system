package com.system.sse.application.sender.service.impl;

import com.system.sse.application.sender.helper.EventDispatcher;
import com.system.sse.application.sender.helper.SseEventFactory;
import com.system.sse.application.sender.model.SseEmitterData;
import com.system.sse.application.sender.service.SubscriptionQueryService;
import com.system.sse.application.sender.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final SubscriptionQueryService subscriptionQueryService;
    private final EventDispatcher dispatcher;
    private final SseEventFactory eventFactory;

    // Topic 별로 clientId 집합을 관리
    private final Map<String, Set<String>> topicSubscribers = new ConcurrentHashMap<>();

    @Override
    public void subscribe(String clientId, String topic) {
        topicSubscribers
                .computeIfAbsent(topic, t -> ConcurrentHashMap.newKeySet())
                .add(clientId);
        log.info("Client {} subscribed to topic {}", clientId, topic);
    }

    @Override
    public void unsubscribe(String clientId, String topic) {
        Set<String> subs = topicSubscribers.get(topic);
        if (subs != null) {
            subs.remove(clientId);
        }
        log.info("Client {} unsubscribed from topic {}", clientId, topic);
    }

    @Override
    public void broadcastToTopic(String topic, SseEmitterData data) {
        Set<String> clients = topicSubscribers.getOrDefault(topic, Collections.emptySet());
        if (clients.isEmpty()) {
            log.debug("No subscribers for topic {}", topic);
            return;
        }
        log.info("Broadcasting event type {} to {} subscribers on topic {}", data.getType(), clients.size(), topic);
        for (String clientId : clients) {
            dispatcher.dispatch(clientId, eventFactory.createDataEvent(data.getType(), data));
        }
    }
}