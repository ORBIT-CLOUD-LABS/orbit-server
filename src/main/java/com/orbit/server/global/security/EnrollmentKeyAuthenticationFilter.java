package com.orbit.server.global.security;

import com.orbit.server.global.error.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 차량 등록 요청의 {@code Authorization: Bearer {enrollment-key}} 가 공통 등록 키와 같으면 요청을 등록 권한으로 인증한다.
 * (VEHICLE-REQ-002 / VEHICLE-POL-003)
 *
 * <p>등록 경로에만 동작한다. 키가 없거나 다르면 다음 필터로 넘기지 않고 {@code 401 INVALID_ENROLLMENT_KEY} 로 바로 응답한다. 차량은
 * 이 코드를 받으면 재시도하지 않고 종료하므로, 인가 단계의 공통 코드(AUTHENTICATION_REQUIRED)로 바뀌면 안 되기 때문이다.
 *
 * <p>필터에서 난 오류는 {@code @RestControllerAdvice} 를 거치지 않으므로, {@link ErrorResponseAuthenticationEntryPoint} 와
 * 같은 이유로 {@link HandlerExceptionResolver} 를 통해 전역 예외 처리기로 넘긴다.
 */
class EnrollmentKeyAuthenticationFilter extends OncePerRequestFilter {

    static final String ENROLLMENT_ROLE = "ENROLLMENT";

    private static final String ENROLLMENT_PRINCIPAL = "enrollment";

    private final RequestMatcher registerPath;
    private final byte[] enrollmentKey;
    private final HandlerExceptionResolver exceptionResolver;

    /**
     * @param registerPath 차량 등록 경로. 경로별 인증 규칙을 한 곳에서 관리하도록 보안 설정이 정해 넘긴다
     * @param properties 공통 등록 키 설정
     * @param exceptionResolver 전역 예외 처리기로 위임하는 MVC 예외 해석기
     */
    EnrollmentKeyAuthenticationFilter(
            RequestMatcher registerPath, EnrollmentProperties properties, HandlerExceptionResolver exceptionResolver) {
        this.registerPath = registerPath;
        this.enrollmentKey = properties.key().getBytes(StandardCharsets.UTF_8);
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !registerPath.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (BearerTokenExtractor.extract(request).filter(this::isEnrollmentKey).isEmpty()) {
            exceptionResolver.resolveException(
                    request, response, null, new BusinessException(AuthErrorCode.INVALID_ENROLLMENT_KEY));
            return;
        }
        RoleAuthentication.establish(ENROLLMENT_PRINCIPAL, ENROLLMENT_ROLE);
        chain.doFilter(request, response);
    }

    // 응답 시간 차이로 키를 한 글자씩 맞혀 볼 수 없도록 일치하는 길이와 상관없이 같은 시간에 비교한다
    private boolean isEnrollmentKey(String presented) {
        return MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), enrollmentKey);
    }
}
