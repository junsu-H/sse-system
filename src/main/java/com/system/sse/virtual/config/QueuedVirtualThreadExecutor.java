package com.system.sse.virtual.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class QueuedVirtualThreadExecutor implements Executor {
    /** 최대 동시에 실행 가능한 Virtual Thread 수를 제한하는 세마포어 */
    private final Semaphore semaphore;
    /** 실행 대기 중인 작업들을 저장하는 블로킹 큐 */
    private final BlockingQueue<Runnable> queue;
    /** Virtual Thread를 생성하고 실행하는 실제 실행기 */
    private final ExecutorService virtualThreadExecutor;
    /** 현재 실행 중인 활성 작업의 수를 추적하는 원자적 정수 */
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    /** Executor가 종료 중인지를 나타내는 휘발성 플래그 */
    private volatile boolean shuttingDown = false;

    public QueuedVirtualThreadExecutor() {
        // CPU 코어 수의 4배를 최대 동시 실행 수로 설정
        int maxConcurrency = Runtime.getRuntime().availableProcessors() * 4;
        // 큐에서 대기할 수 있는 최대 작업 수를 1000개로 설정
        int maxQueueSize = 1000;

        // 설정된 최대 동시 실행 수로 세마포어 초기화
        this.semaphore = new Semaphore(maxConcurrency);
        // 설정된 크기로 연결된 블로킹 큐 생성
        this.queue = new LinkedBlockingQueue<>(maxQueueSize);

        // Virtual Thread 전용 실행기를, 이름 패턴 "vt-task-0", "vt-task-1", ... 으로 생성
        this.virtualThreadExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("vt-task-", 0).factory()
        );
        // 큐에서 작업을 소비할 데몬 스레드 생성
        Thread queueConsumer = new Thread(this::consumeQueue, "queue-consumer");
        // 데몬 스레드로 설정하여 메인 애플리케이션 종료 시 함께 종료
        queueConsumer.setDaemon(true);
        // 큐 소비자 스레드 시작
        queueConsumer.start();

        // 초기화 완료 로그 출력 (최대 동시 실행 수와 큐 크기 포함)
        log.info("QueuedVirtualThreadExecutor initialized (maxConcurrency={}, maxQueueSize={})",
                maxConcurrency, maxQueueSize);
    }

    @Override
    public void execute(Runnable command) {
        if (shuttingDown) {
            throw new RejectedExecutionException("Executor is shutting down");
        }
        if (!queue.offer(command)) {
            throw new RejectedExecutionException("Task queue is full");
        }
    }

    private void consumeQueue() {
        try {
            while (!shuttingDown || !queue.isEmpty()) {
                Runnable task = queue.poll(200, TimeUnit.MILLISECONDS);
                if (task != null) {
                    semaphore.acquire();
                    activeTasks.incrementAndGet();

                    virtualThreadExecutor.execute(() -> {
                        try {
                            task.run();
                        } catch (Throwable t) {
                            log.error("Virtual thread task failed", t);
                        } finally {
                            activeTasks.decrementAndGet();
                            semaphore.release();
                        }
                    });
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Queue consumer interrupted");
        }
    }

    public int getActiveTasks() {
        return activeTasks.get();
    }

    public int getQueueSize() {
        return queue.size();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down QueuedVirtualThreadExecutor...");
        shuttingDown = true;

        try {
            virtualThreadExecutor.shutdown();
            if (!virtualThreadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                virtualThreadExecutor.shutdownNow();
            }
            log.info("QueuedVirtualThreadExecutor shut down cleanly");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            virtualThreadExecutor.shutdownNow();
            log.warn("Shutdown interrupted, forced termination");
        }
    }
}
