package com.system.sse.application.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class ExecutorLifecycle implements SmartLifecycle {
    private static final Duration GRACEFUL_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration FORCE_TIMEOUT = Duration.ofSeconds(10);

    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("ExecutorLifecycle.start: VirtualThreadExecutor started");
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            log.info("ExecutorLifecycle.stop: VirtualThreadExecutor already stopped or stopping");
            return; // 이미 종료 중이거나 종료됨
        }

        log.info("ExecutorLifecycle.stop: Initiating VirtualThreadExecutor shutdown");
        try {
            executor.shutdown();

            if (executor.awaitTermination(GRACEFUL_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                log.info("ExecutorLifecycle.stop: VirtualThreadExecutor gracefully terminated");
                return;
            }

            log.warn("ExecutorLifecycle.stop: Graceful shutdown timed out, forcing termination");
            var unfinishedTasks = executor.shutdownNow();
            log.warn("ExecutorLifecycle.stop: Forced shutdown cancelled {} unfinished tasks", unfinishedTasks.size());

            if (executor.awaitTermination(FORCE_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                log.info("ExecutorLifecycle.stop: VirtualThreadExecutor forcefully terminated");
            } else {
                log.error("ExecutorLifecycle.stop: VirtualThreadExecutor failed to terminate within timeout");
            }

        } catch (InterruptedException e) {
            log.warn("ExecutorLifecycle.stop: Interrupted during shutdown, forcing immediate termination");
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get() && !executor.isShutdown();
    }

    /**
     * 높을수록 가장 나중에 종료됨.
     *
     * @return
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE / 2;
    }


    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }
}