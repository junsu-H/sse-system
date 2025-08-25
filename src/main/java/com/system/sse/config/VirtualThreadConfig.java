package com.system.sse.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class VirtualThreadConfig {
    private ExecutorService executor;

    @Bean
    public ExecutorService virtualThreadExecutor() {
        this.executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("vt-task-", 0).factory()
        );
        log.info("VirtualThreadExecutor initialized");

        return executor;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down VirtualThreadExecutor...");
        executor.shutdown();
    }
}
