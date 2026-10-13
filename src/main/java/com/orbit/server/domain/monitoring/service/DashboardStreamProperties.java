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
 * <p>간격이 0 이하면 스케줄러가 쉬지 않고 돈다. 겹침 시간이 체크인 트랜잭션 최대 시간보다 짧으면 늦게 커밋된 변경을 SSE 에서 영구히
 * 놓친다. 그래서 값이 올바르지 않으면 앱을 띄우지 않는다.
 *
 * @param pollInterval 변경분 조회 주기. 환경 변수 {@code DASHBOARD_STREAM_POLL_INTERVAL} 로 바꾼다
 * @param keepAliveInterval keep-alive 주석을 보내는 간격. 환경 변수 {@code DASHBOARD_STREAM_KEEP_ALIVE_INTERVAL} 로 바꾼다
 * @param overlap 변경분을 이전 기준 시각보다 앞당겨 겹쳐 조회할 시간. {@link #MIN_OVERLAP_SECONDS} 초 이상이어야 한다. 환경 변수
 *     {@code DASHBOARD_STREAM_OVERLAP} 로 바꾼다
 */
@Validated
@ConfigurationProperties("orbit.dashboard.stream")
public record DashboardStreamProperties(
        @NotNull @DurationMin(millis = 1) Duration pollInterval,
        @NotNull @DurationMin(millis = 1) Duration keepAliveInterval,

        @NotNull @DurationMin(seconds = DashboardStreamProperties.MIN_OVERLAP_SECONDS)
        Duration overlap) {

    /**
     * 설정할 수 있는 가장 짧은 겹침 시간(초). 체크인 트랜잭션 타임아웃(3초)보다 길어야 하고, 이는 CheckInLateCommitTest 가 확인한다.
     */
    // 트랜잭션 타임아웃은 SQL 문 단위로 확인해 커밋 지연까지는 막지 못하므로 타임아웃과 같은 값이 아니라 여유를 더한다.
    // update 컨텍스트의 타임아웃 상수를 직접 참조하면 다른 컨텍스트 내부에 묶이므로 값을 따로 두고 테스트로 맞춘다
    public static final long MIN_OVERLAP_SECONDS = 5;
}
