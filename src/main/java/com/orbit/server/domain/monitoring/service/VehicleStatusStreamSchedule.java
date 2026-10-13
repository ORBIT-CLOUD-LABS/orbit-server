package com.orbit.server.domain.monitoring.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 대시보드 SSE 의 주기 작업을 등록한다. 변경분 조회와 keep-alive 는 pod 마다 하나씩만 돈다. (MONITORING-REQ-001)
 *
 * <p>{@code @Scheduled} 문자열 대신 여기서 등록해 검증을 거친 설정의 간격을 그대로 쓴다.
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
class VehicleStatusStreamSchedule implements SchedulingConfigurer {

    private final VehicleStatusBroadcaster broadcaster;
    private final VehicleStatusSubscribers subscribers;
    private final DashboardStreamProperties properties;

    // 조회가 주기보다 오래 걸려도 겹쳐 돌지 않도록 이전 조회가 끝난 뒤부터 간격을 센다
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedDelayTask(broadcaster::broadcastChanges, properties.pollInterval());
        registrar.addFixedRateTask(subscribers::sendKeepAlive, properties.keepAliveInterval());
    }
}
