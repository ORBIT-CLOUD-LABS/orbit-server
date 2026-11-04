package com.orbit.server.domain.edge.service;

import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Edge 헬스체크를 전용 스레드 하나에서 주기적으로 돌린다.
 *
 * <p>Spring 기본 스케줄러는 스레드가 하나라 대시보드 SSE 조회·keep-alive 와 함께 쓴다. 응답 없는 Edge 를 타임아웃까지 기다리는 동안 SSE 가
 * 멈추지 않도록 따로 돌린다. 전용 스케줄러를 빈으로 등록하면 Spring Boot 가 기본 스케줄러를 만들지 않고 다른 주기 작업까지 이 스케줄러로 옮겨
 * 가므로, 빈이 아니라 여기서 직접 만들고 수명을 관리한다.
 */
@Component
class EdgeHealthCheckSchedule implements SmartLifecycle {

    static final String THREAD_NAME_PREFIX = "edge-health-check-";

    private final EdgeHealthCheckService checkService;
    private final EdgeHealthCheckProperties properties;
    private ThreadPoolTaskScheduler scheduler;

    EdgeHealthCheckSchedule(EdgeHealthCheckService checkService, EdgeHealthCheckProperties properties) {
        this.checkService = checkService;
        this.properties = properties;
    }

    // 확인이 주기보다 오래 걸려도 겹쳐 돌지 않도록 이전 확인이 끝난 뒤부터 간격을 센다
    @Override
    public void start() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix(THREAD_NAME_PREFIX);
        scheduler.initialize();
        scheduler.scheduleWithFixedDelay(checkService::checkAll, properties.interval());
    }

    // 종료 중에는 진행 중인 호출을 기다리지 않는다. 판정은 Pod 메모리에만 있어 끝까지 기록할 이유가 없다
    @Override
    public void stop() {
        if (scheduler == null) {
            return;
        }
        scheduler.shutdown();
        scheduler = null;
    }

    @Override
    public boolean isRunning() {
        return scheduler != null;
    }
}
