package com.system.sse.config;

import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Service
public class CpuTaskThreadConfig {
    private final ExecutorService vts;

    private final Semaphore limiter;

    public CpuTaskThreadConfig(ExecutorService vts) {
        this.vts = vts;
        // CPU만큼 동시 실행 제한
        this.limiter = new Semaphore(Runtime.getRuntime().availableProcessors());
    }

    public CompletableFuture<Void> doHeavy(Runnable task) {
        return CompletableFuture.runAsync(() -> {
            try {
                limiter.acquire();
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                limiter.release();
            }
        }, vts);
    }
}