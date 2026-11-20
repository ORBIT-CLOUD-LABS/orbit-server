package com.orbit.server.domain.edge.service.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.orbit.server.domain.edge.service.EdgeHealthCheckService;
import com.orbit.server.domain.edge.service.config.EdgeHealthCheckProperties;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EdgeHealthCheckSchedulerTest {

    private static final Duration INTERVAL = Duration.ofMillis(20);

    private final AtomicInteger checks = new AtomicInteger();
    private final Set<String> threadNames = ConcurrentHashMap.newKeySet();
    private final EdgeHealthCheckScheduler scheduler = schedulerCountingChecks();

    @AfterEach
    void stopScheduler() {
        scheduler.stop();
    }

    @Test
    @DisplayName("시작하면 설정한 주기로 Edge 를 반복해서 확인한다")
    void checksRepeatedlyAfterStart() {
        scheduler.start();

        await().atMost(Duration.ofSeconds(2)).until(() -> checks.get() >= 3);
        assertThat(scheduler.isRunning()).isTrue();
    }

    // 기본 스케줄러를 쓰면 응답 없는 Edge 를 기다리는 동안 대시보드 SSE 조회·keep-alive 가 함께 멈춘다
    @Test
    @DisplayName("Spring 기본 스케줄러가 아닌 헬스체크 전용 스레드에서 확인한다")
    void checksOnDedicatedThread() {
        scheduler.start();

        await().atMost(Duration.ofSeconds(2)).until(() -> checks.get() >= 1);
        assertThat(threadNames).allMatch(name -> name.startsWith(EdgeHealthCheckScheduler.THREAD_NAME_PREFIX));
    }

    @Test
    @DisplayName("멈추면 더 이상 확인하지 않는다")
    void stopsChecking() throws InterruptedException {
        scheduler.start();
        await().atMost(Duration.ofSeconds(2)).until(() -> checks.get() >= 1);

        scheduler.stop();
        int checksAtStop = checks.get();
        Thread.sleep(INTERVAL.multipliedBy(5));

        assertThat(scheduler.isRunning()).isFalse();
        assertThat(checks.get()).isEqualTo(checksAtStop);
    }

    // 확인 내용은 EdgeHealthCheckServiceTest 가 검증한다. 여기서는 언제·어느 스레드에서 불리는지만 본다
    private EdgeHealthCheckScheduler schedulerCountingChecks() {
        EdgeHealthCheckService checkService = mock(EdgeHealthCheckService.class);
        doAnswer(invocation -> {
                    threadNames.add(Thread.currentThread().getName());
                    checks.incrementAndGet();
                    return null;
                })
                .when(checkService)
                .checkAll();
        return new EdgeHealthCheckScheduler(checkService, new EdgeHealthCheckProperties(INTERVAL, 3));
    }
}
