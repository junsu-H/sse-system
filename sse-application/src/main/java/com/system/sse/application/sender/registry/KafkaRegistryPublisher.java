package com.system.sse.application.sender.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Profile("kafka")
@Component
@RequiredArgsConstructor
public class KafkaRegistryPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String REGISTRY_TOPIC = "client-registry";

    public void publishRegister(String clientId) {
        kafkaTemplate.send(REGISTRY_TOPIC, clientId, "REGISTER");
    }

    public void publishRemove(String clientId) {
        kafkaTemplate.send(REGISTRY_TOPIC, clientId, "REMOVE");
    }
}