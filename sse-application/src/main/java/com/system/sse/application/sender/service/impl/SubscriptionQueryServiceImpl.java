package com.system.sse.application.sender.service.impl;

import com.system.sse.application.sender.registry.LocalSseEmitterRegistry;
import com.system.sse.application.sender.service.SubscriptionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

    private final LocalSseEmitterRegistry registry;

    /**
     * topic 별로 clientId 집합을 유지하는 맵.
     * key: topic, value: clientId 집합
     */
    private final Map<String, Set<String>> topicSubscriptions = new ConcurrentHashMap<>();

    @Override
    public Set<String> listAllClients() {
        return Collections.unmodifiableSet(registry.getAllClientIds());
    }

    @Override
    public Set<String> listClientsByTopic(String topic) {
        return topicSubscriptions.getOrDefault(topic, Collections.emptySet());
    }

    /**
     * 클라이언트를 특정 topic 에 등록
     */
    public void subscribe(String clientId, String topic) {
        topicSubscriptions
                .computeIfAbsent(topic, t -> ConcurrentHashMap.newKeySet())
                .add(clientId);
    }

    /**
     * 클라이언트를 특정 topic 에서 해제
     */
    public void unsubscribe(String clientId, String topic) {
        topicSubscriptions.getOrDefault(topic, Collections.emptySet())
                .remove(clientId);
    }
}
