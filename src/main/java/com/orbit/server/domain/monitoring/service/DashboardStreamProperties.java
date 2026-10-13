package com.orbit.server.domain.monitoring.service;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 대시보드 차량 상태 SSE 설정. keep-alive 간격은 아직 협의 중이고 조회 주기·겹침 시간은 배포 환경의 부하와 체크인 처리 시간에 맞춰야
 * 하므로 설정으로 둔다. (MONITORING-REQ-001 / MONITORING-POL-001)
 *
 * <p>간격이 0 이하면 스케줄러가 쉬지 않고 돌거나 겹침 조회가 늦게 커밋된 변경을 놓치므로 값이 올바르지 않으면 앱을 띄우지 않는다.
 *
 * @param pollInterval 변경분 조회 주기. 환경 변수 {@code DASHBOARD_STREAM_POLL_INTERVAL} 로 바꾼다
 * @param keepAliveInterval keep-alive 주석을 보내는 간격. 환경 변수 {@code DASHBOARD_STREAM_KEEP_ALIVE_INTERVAL} 로 바꾼다
 * @param overlap 변경분을 이전 기준 시각보다 앞당겨 겹쳐 조회할 시간. 체크인 트랜잭션 최대 시간보다 길어야 한다. 환경 변수
 *     {@code DASHBOARD_STREAM_OVERLAP} 로 바꾼다
 */
@Validated
@ConfigurationProperties("orbit.dashboard.stream")
public record DashboardStreamProperties(
        @NotNull @DurationMin(millis = 1) Duration pollInterval,
        @NotNull @DurationMin(millis = 1) Duration keepAliveInterval,
        @NotNull @DurationMin(millis = 1) Duration overlap) {}
