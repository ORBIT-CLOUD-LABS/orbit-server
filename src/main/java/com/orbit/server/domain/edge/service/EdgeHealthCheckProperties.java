package com.orbit.server.domain.edge.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Edge 헬스체크 설정. 주기와 실패 기준은 아직 협의 중이라(flow.md §7) 배포 설정으로 바꿀 수 있게 둔다.
 *
 * <p>주기가 0 이하면 스케줄러가 쉬지 않고 돌고, 실패 기준이 1 보다 작으면 모든 Edge 가 처음부터 비정상이 된다. 그래서 값이 올바르지 않으면 앱을
 * 띄우지 않는다. {@code /healthz} 호출 타임아웃은 HTTP 호출 구현의 설정이라 {@code orbit.cdn.health-check.timeout} 으로 따로 읽는다.
 *
 * @param interval 이전 확인이 끝난 뒤 다음 확인까지의 간격. 환경 변수 {@code CDN_HEALTH_CHECK_INTERVAL} 로 바꾼다
 * @param failureThreshold 비정상으로 볼 연속 실패 횟수. 1 이상. 환경 변수 {@code CDN_HEALTH_CHECK_FAILURE_THRESHOLD} 로
 *     바꾼다
 */
@Validated
@ConfigurationProperties("orbit.cdn.health-check")
public record EdgeHealthCheckProperties(
        @NotNull @DurationMin(millis = 1) Duration interval,
        @NotNull @Min(1) Integer failureThreshold) {}
