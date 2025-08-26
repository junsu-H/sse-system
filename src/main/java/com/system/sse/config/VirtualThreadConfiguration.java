package com.system.sse.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
@ConditionalOnThreading(Threading.VIRTUAL)
public class VirtualThreadConfiguration {

    @Bean(destroyMethod = "close")
    public ExecutorService virtualThreadExecutor() {
        log.info("VirtualThreadConfiguration.virtualThreadExecutor: Creating VirtualThreadExecutor");
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual()
                        .name("vt-task-", 0)
                        .uncaughtExceptionHandler(this::handleUncaughtException)
                        .factory()
        );
    }

    @Bean
    public ExecutorLifecycle executorLifecycle(ExecutorService virtualThreadExecutor) {
        return new ExecutorLifecycle(virtualThreadExecutor);
    }

    private void handleUncaughtException(Thread thread, Throwable e) {
        log.error("VirtualThreadConfiguration.handleUncaughtException: Uncaught exception in thread {}: {}", thread.getName(), e.getMessage(), e);

        // TODO: Datadog, Micrometer 등 모니터링 시스템 연동
    }
}
