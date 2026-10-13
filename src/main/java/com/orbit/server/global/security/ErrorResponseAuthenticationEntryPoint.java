package com.orbit.server.global.security;

import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증이 필요한 경로에 인증 없이 들어온 요청에 {@code 401 AUTHENTICATION_REQUIRED} 를 공통 오류 형식으로 응답한다.
 *
 * <p>필터에서 난 예외는 {@code @RestControllerAdvice} 를 거치지 않으므로, 응답 형식과 로그 규칙을 한 곳에서 관리하도록
 * {@link HandlerExceptionResolver} 를 통해 전역 예외 처리기로 넘긴다. 요청 헤더나 인증 예외 메시지는 응답·로그에 옮기지 않아
 * 토큰 값이 남지 않는다.
 */
class ErrorResponseAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver exceptionResolver;

    /**
     * @param exceptionResolver 전역 예외 처리기로 위임하는 MVC 예외 해석기
     */
    ErrorResponseAuthenticationEntryPoint(HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        exceptionResolver.resolveException(
                request, response, null, new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED));
    }
}
