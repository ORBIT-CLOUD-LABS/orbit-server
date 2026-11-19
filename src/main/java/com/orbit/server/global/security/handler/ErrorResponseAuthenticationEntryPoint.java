package com.orbit.server.global.security.handler;

import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 인증이 필요한 경로에 인증 없이 들어온 요청에 {@code 401 AUTHENTICATION_REQUIRED} 를 공통 오류 형식으로 응답한다.
 *
 * <p>필터에서 난 예외는 {@code @RestControllerAdvice} 를 거치지 않으므로, 응답 형식과 로그 규칙을 한 곳에서 관리하도록
 * {@link SecurityErrorResponder} 를 통해 전역 예외 처리기로 넘긴다. 요청 헤더나 인증 예외 메시지는 응답·로그에 옮기지 않아
 * 토큰 값이 남지 않는다.
 */
public class ErrorResponseAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponder errorResponder;

    /**
     * @param errorResponder 전역 예외 처리기로 오류 응답을 위임하는 응답기
     */
    public ErrorResponseAuthenticationEntryPoint(SecurityErrorResponder errorResponder) {
        this.errorResponder = errorResponder;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        errorResponder.respond(request, response, new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED));
    }
}
