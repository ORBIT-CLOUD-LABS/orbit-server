package com.orbit.server.global.security;

import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증은 됐지만 경로에 접근할 권한이 없는 요청에 {@code 403 ACCESS_DENIED} 를 공통 오류 형식으로 응답한다.
 *
 * <p>{@link ErrorResponseAuthenticationEntryPoint} 와 같은 이유로 {@link HandlerExceptionResolver} 를 통해 전역 예외
 * 처리기로 넘긴다. 기본 처리기는 Spring Boot 오류 형식으로 응답해 다른 API 오류와 형식이 달라진다.
 */
class ErrorResponseAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver exceptionResolver;

    /**
     * @param exceptionResolver 전역 예외 처리기로 위임하는 MVC 예외 해석기
     */
    ErrorResponseAccessDeniedHandler(HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        exceptionResolver.resolveException(
                request, response, null, new BusinessException(CommonErrorCode.ACCESS_DENIED));
    }
}
