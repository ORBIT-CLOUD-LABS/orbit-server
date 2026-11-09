package com.orbit.server.domain.edge.service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 매니페스트가 차량에 내려줄 Edge 를 고르는 규칙의 설정.
 *
 * <p>fallback 개수가 음수면 목록을 자를 수 없고, 위치 유효 시간이 0 이하면 모든 위치가 오래된 것이 되어 항상 기본 Edge 만 고르게 된다.
 * 그래서 값이 올바르지 않으면 앱을 띄우지 않는다.
 *
 * @param fallbackCount 매니페스트 fallbackUrls 에 넣을 예비 Edge 개수. 0 이상. 환경 변수 {@code CDN_FALLBACK_COUNT} 로 바꾼다
 * @param locationMaxAge 이보다 오래된 차량 위치는 Edge 선택에 쓰지 않는다. 0 보다 길어야 한다. 환경 변수 {@code
 *     CDN_LOCATION_MAX_AGE} 로 바꾼다
 */
@Validated
@ConfigurationProperties("orbit.cdn")
public record EdgeSelectionProperties(
        @NotNull @PositiveOrZero Integer fallbackCount,
        @NotNull @DurationMin(millis = 1) Duration locationMaxAge) {}
