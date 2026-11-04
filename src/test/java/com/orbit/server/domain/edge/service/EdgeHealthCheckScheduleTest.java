package com.orbit.server.domain.edge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.orbit.server.domain.edge.entity.Coordinates;
import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.entity.Edges;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EdgeHealthCheckScheduleTest {

    private static final Duration INTERVAL = Duration.ofMillis(20);
    private static final Edge SEOUL =
            new Edge("edge-seoul", "서울", URI.create("http://cdn.test:30081"), new Coordinates(37.5665, 126.9780));

    private final AtomicInteger checks = new AtomicInteger();
    private final Set<String> threadNames = ConcurrentHashMap.newKeySet();
    private final EdgeHealthCheckSchedule schedule = scheduleCountingChecks();

    @AfterEach
    void stopSchedule() {
        schedule.stop();
    }

    @Test
    @DisplayName("시작하면 설정한 주기로 Edge 를 반복해서 확인한다")
    void checksRepeatedlyAfterStart() {
        schedule.start();

        await().atMost(Duration.ofSeconds(2)).until(() -> checks.get() >= 3);
        assertThat(schedule.isRunning()).isTrue();
    }

    // 기본 스케줄러를 쓰면 응답 없는 Edge 를 기다리는 동안 대시보드 SSE 조회·keep-alive 가 함께 멈춘다
    @Test
    @DisplayName("Spring 기본 스케줄러가 아닌 헬스체크 전용 스레드에서 확인한다")
    void checksOnDedicatedThread() {
        schedule.start();

        await().atMost(Duration.ofSeconds(2)).until(() -> checks.get() >= 1);
        assertThat(threadNames).allMatch(name -> name.startsWith(EdgeHealthCheckSchedule.THREAD_NAME_PREFIX));
    }

    @Test
    @DisplayName("멈추면 더 이상 확인하지 않는다")
    void stopsChecking() throws InterruptedException {
        schedule.start();
        await().atMost(Duration.ofSeconds(2)).until(() -> checks.get() >= 1);

        schedule.stop();
        int checksAtStop = checks.get();
        Thread.sleep(INTERVAL.multipliedBy(5));

        assertThat(schedule.isRunning()).isFalse();
        assertThat(checks.get()).isEqualTo(checksAtStop);
    }

    private EdgeHealthCheckSchedule scheduleCountingChecks() {
        EdgeCatalog catalog = () -> new Edges(List.of(SEOUL), "edge-seoul");
        EdgeHealthCheckProperties properties = new EdgeHealthCheckProperties(INTERVAL, 3);
        EdgeHealthProbe countingProbe = edge -> {
            threadNames.add(Thread.currentThread().getName());
            checks.incrementAndGet();
            return true;
        };
        EdgeHealthBoard board = new EdgeHealthBoard(catalog, properties);
        return new EdgeHealthCheckSchedule(new EdgeHealthCheckService(catalog, countingProbe, board), properties);
    }
}
