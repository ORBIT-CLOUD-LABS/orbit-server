package com.orbit.server.global.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 관리자·대시보드 API 의 관리자 토큰 설정. Phase1 은 발급 절차 없이 환경 변수로 주입한 고정 토큰 하나를 쓴다.
 *
 * <p>토큰이 비어 있으면 관리자 API 가 열린 채로 뜰 수 있으므로 값이 없으면 앱을 띄우지 않는다.
 *
 * @param token 관리자 토큰. 환경 변수 {@code ADMIN_TOKEN} 으로 주입한다
 */
@Validated
@ConfigurationProperties("orbit.admin")
record AdminProperties(@NotBlank String token) {

    private static final String MASKED = "AdminProperties[token=****]";

    // 설정 객체가 로그·디버그 출력에 찍혀도 토큰이 남지 않도록 레코드 기본 toString 을 덮는다
    @Override
    public String toString() {
        return MASKED;
    }
}
