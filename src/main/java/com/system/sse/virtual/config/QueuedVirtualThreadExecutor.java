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
        // Virtual Thread 전용 실행기 생성
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // 큐에서 작업을 소비할 데몬 스레드 생성
        Thread queueConsumer = new Thread(this::consumeQueue);
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
        // Executor가 종료 중인지 확인
        if (shuttingDown) {
            // 종료 중이면 작업 거부 예외 발생
            throw new RejectedExecutionException("Executor is shutting down");
        }
        // 큐에 작업 추가 시도 (큐가 가득 차면 false 반환)
        if (!queue.offer(command)) {
            // 큐가 가득 찬 경우 작업 거부 예외 발생
            throw new RejectedExecutionException("Task queue is full");
        }
    }

    private void consumeQueue() {
        try {
            // 종료되지 않았거나 큐에 남은 작업이 있는 동안 계속 실행
            while (!shuttingDown || !queue.isEmpty()) {
                // 큐에서 200밀리초 동안 작업을 기다리며 폴링
                Runnable task = queue.poll(200, TimeUnit.MILLISECONDS);
                // 작업을 성공적으로 가져온 경우
                if (task != null) {
                    // 세마포어에서 허가 획득 (최대 동시 실행 수 제한)
                    semaphore.acquire();
                    // 활성 작업 수 증가
                    activeTasks.incrementAndGet();

                    // Virtual Thread로 실제 작업 실행
                    virtualThreadExecutor.execute(() -> {
                        try {
                            // 사용자가 제출한 실제 작업 실행
                            task.run();
                        } catch (Throwable t) {
                            // 작업 실행 중 발생한 모든 예외와 에러를 로깅
                            log.error("Virtual thread task failed", t);
                        } finally {
                            // 작업 완료 후 활성 작업 수 감소
                            activeTasks.decrementAndGet();
                            // 세마포어 허가 반납 (다른 작업이 실행될 수 있도록)
                            semaphore.release();
                        }
                    });
                }
            }
        } catch (InterruptedException e) {
            // 스레드 인터럽트 시 인터럽트 상태 복원
            Thread.currentThread().interrupt();
            // 큐 소비자가 인터럽트되었음을 경고 로그로 기록
            log.warn("Queue consumer interrupted");
        }
    }

    /** 현재 실행 중인 활성 작업 수를 반환하는 모니터링 메서드 */
    public int getActiveTasks() {
        // 원자적 정수에서 현재 값 반환
        return activeTasks.get();
    }

    /** 현재 큐에 대기 중인 작업 수를 반환하는 모니터링 메서드 */
    public int getQueueSize() {
        // 큐의 현재 크기 반환
        return queue.size();
    }

    /** Spring 컨테이너 종료 시 호출되는 우아한 종료 메서드 */
    @PreDestroy
    public void shutdown() {
        // 종료 시작 로그 출력
        log.info("Shutting down QueuedVirtualThreadExecutor...");
        // 종료 플래그를 true로 설정하여 새 작업 거부 및 큐 소비 중단 신호
        shuttingDown = true;

        try {
            // Virtual Thread 실행기에 종료 신호 전송
            virtualThreadExecutor.shutdown();
            // 30초 동안 모든 작업이 완료되기를 기다림
            if (!virtualThreadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                // 30초 내에 종료되지 않으면 강제 종료 시도
                virtualThreadExecutor.shutdownNow();
            }
            // 정상적으로 종료된 경우 로그 출력
            log.info("QueuedVirtualThreadExecutor shut down cleanly");
        } catch (InterruptedException e) {
            // 종료 대기 중 인터럽트 발생 시 현재 스레드의 인터럽트 상태 복원
            Thread.currentThread().interrupt();
            // 강제 종료 실행
            virtualThreadExecutor.shutdownNow();
            // 인터럽트로 인한 강제 종료임을 경고 로그로 기록
            log.warn("Shutdown interrupted, forced termination");
        }
    }
}
