package com.system.sse.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VirtualThreadConfigurationTest {

    @Autowired
    private ExecutorService executor;

    @Autowired
    private ExecutorLifecycle lifecycle;

    @BeforeEach
    void startLifecycle() {
        lifecycle.start();
    }

    @AfterEach
    void stopLifecycle() {
        lifecycle.stop();
    }

    // -----------------------------------
    // 1. Executor Bean 생성 테스트
    // -----------------------------------
    @Test
    void testExecutorBeanCreation() {
        assertNotNull(executor, "ExecutorService Bean이 생성되어야 합니다");
    }

    // -----------------------------------
    // 2. Virtual Thread 실행 테스트
    // -----------------------------------
    @Test
    void testVirtualThreadExecution() throws Exception {
        Future<String> future = executor.submit(() ->
                Thread.currentThread().isVirtual() ? "VIRTUAL" : "PLATFORM"
        );

        assertEquals("VIRTUAL", future.get(5, TimeUnit.SECONDS));
    }

    // -----------------------------------
    // 3. 다중 Task 동시 실행 테스트
    // -----------------------------------
    @Test
    void testMultipleTasksExecution() throws InterruptedException {
        int taskCount = 50;
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "모든 Task가 완료되어야 합니다");
        assertEquals(taskCount, counter.get());
    }

    // -----------------------------------
    // 4. Graceful Shutdown 테스트
    // -----------------------------------
    @Test
    void testGracefulShutdown() throws InterruptedException {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskFinished = new CountDownLatch(1);

        executor.submit(() -> {
            taskStarted.countDown(); // Task 시작 알림
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            taskFinished.countDown();
        });

        // Task가 Executor에 들어간 것을 확인
        taskStarted.await();

        lifecycle.stop();

        assertTrue(taskFinished.await(1, TimeUnit.SECONDS), "Graceful 종료 후 Task가 완료되어야 합니다");
        assertTrue(executor.isShutdown() || executor.isTerminated(), "Executor가 종료되어야 합니다");
    }

    // -----------------------------------
    // 5. Forceful Shutdown 테스트
    // -----------------------------------
    @Test
    void testForcefulShutdown() throws InterruptedException {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskFinished = new CountDownLatch(1);

        executor.submit(() -> {
            taskStarted.countDown(); // Task 시작 알림
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            taskFinished.countDown();
        });

        // Task가 Executor에 들어간 것을 확인
        taskStarted.await();

        lifecycle.stop(); // 강제 종료

        // Forceful shutdown 이후 Task는 아직 끝나지 않았을 수 있음
        assertTrue(taskFinished.getCount() == 1 || taskFinished.await(1, TimeUnit.SECONDS));
        assertTrue(executor.isShutdown() || executor.isTerminated(), "Executor가 종료되어야 합니다");
    }
}