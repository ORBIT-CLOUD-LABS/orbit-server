package com.orbit.server.domain.update.service;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 체크인 설정. 체크인 주기는 아직 협의 중이라 배포 환경마다 바꿀 수 있도록 설정으로 둔다. (VEHICLE-REQ-001)
 *
 * <p>주기가 0 이하면 차량이 쉬지 않고 체크인하므로 값이 올바르지 않으면 앱을 띄우지 않는다.
 *
 * @param intervalSeconds 차량이 다음 체크인까지 기다릴 시간(초). 환경 변수 {@code CHECK_IN_INTERVAL_SECONDS} 로 바꾼다
 */
@Validated
@ConfigurationProperties("orbit.check-in")
public record CheckInProperties(@Positive int intervalSeconds) {}
