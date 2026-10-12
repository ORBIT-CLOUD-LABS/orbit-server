package com.orbit.server.global.security;

import com.orbit.server.global.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 차량 등록 요청의 {@code Authorization: Bearer {enrollment-key}} 를 공통 등록 키와 비교한다. (VEHICLE-REQ-002 /
 * VEHICLE-POL-003)
 *
 * <p>등록 경로에만 건다. 다른 API 는 각자의 인증(차량 토큰, 관리자 토큰)이 등록 키를 거부한다.
 */
class EnrollmentKeyInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final byte[] enrollmentKey;

    /**
     * @param properties 공통 등록 키 설정
     */
    EnrollmentKeyInterceptor(EnrollmentProperties properties) {
        this.enrollmentKey = properties.key().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @throws BusinessException 등록 키가 없거나 다르면 INVALID_ENROLLMENT_KEY
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!isValid(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            throw new BusinessException(AuthErrorCode.INVALID_ENROLLMENT_KEY);
        }
        return true;
    }

    private boolean isValid(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return false;
        }
        byte[] presented = authorization.substring(BEARER_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        // 응답 시간 차이로 키를 한 글자씩 맞혀 볼 수 없도록 일치하는 길이와 상관없이 같은 시간에 비교한다
        return MessageDigest.isEqual(presented, enrollmentKey);
    }
}
