package com.system.sse.application.receiver;

import com.system.sse.application.sender.registry.LocalSseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("kafka")
@Component
@RequiredArgsConstructor
public class KafkaRegistryListener {
    private final LocalSseEmitterRegistry localCache;

    @KafkaListener(topics = "client-registry", groupId = "sse-registry-group")
    public void onRegistryEvent(ConsumerRecord<String, String> record) {
        String clientId = record.key();
        String action = record.value();
        if ("REMOVE".equals(action)) {
            localCache.remove(clientId);
            log.info("Removed client from cache: {}", clientId);
        }
        // REGISTER handled by publisher side updating LocalCache directly
    }
}