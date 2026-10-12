package com.orbit.server.global.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 차량 등록 키 설정. 모든 차량이 같은 키로 등록하고, 등록 후에는 차량별 토큰을 쓴다. (VEHICLE-POL-003)
 *
 * <p>키가 비어 있으면 아무 차량이나 등록할 수 있으므로 값이 없으면 앱을 띄우지 않는다.
 *
 * @param key 공통 등록 키. 환경 변수 {@code ENROLLMENT_KEY} 로 주입한다
 */
@Validated
@ConfigurationProperties("orbit.enrollment")
record EnrollmentProperties(@NotBlank String key) {}
