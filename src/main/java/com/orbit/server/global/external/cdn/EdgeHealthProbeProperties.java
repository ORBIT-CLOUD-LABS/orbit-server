package com.orbit.server.global.external.cdn;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Edge {@code /healthz} 호출 설정. 주기·실패 기준(도메인 설정)과 같은 {@code orbit.cdn.health-check} 아래에 두지만, 타임아웃은 HTTP 호출
 * 구현의 일이라 여기서 따로 읽는다.
 *
 * <p>응답하지 않는 Edge 를 기다리는 동안 다른 Edge 확인이 밀리므로 타임아웃은 헬스체크 주기보다 충분히 짧아야 한다. 0 이하면 기다림에 제한이
 * 없어지므로 앱을 띄우지 않는다.
 *
 * @param timeout 연결과 응답을 각각 기다리는 최대 시간. 환경 변수 {@code CDN_HEALTH_CHECK_TIMEOUT} 로 바꾼다
 */
@Validated
@ConfigurationProperties("orbit.cdn.health-check")
record EdgeHealthProbeProperties(
        @NotNull @DurationMin(millis = 1) Duration timeout) {}
