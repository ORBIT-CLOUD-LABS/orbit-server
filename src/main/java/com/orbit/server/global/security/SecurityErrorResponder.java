package com.orbit.server.global.security;

import com.orbit.server.global.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 보안 필터 단계에서 난 오류를 전역 예외 처리기로 넘겨 공통 오류 형식으로 응답하게 한다.
 *
 * <p>필터에서 난 예외는 {@code @RestControllerAdvice} 를 거치지 않으므로, 응답 형식과 로그 규칙을 한 곳에서 관리하도록
 * {@link HandlerExceptionResolver} 로 위임한다. 요청 헤더나 예외 메시지는 응답·로그에 옮기지 않아 토큰 값이 남지 않는다.
 *
 * <p>해석기가 응답을 만들지 못하면(처리기가 없거나 처리기 자체가 실패) 응답이 손대지 않은 200 으로 남아, 거부해야 할 요청이 성공처럼
 * 끝난다. 그래서 그때는 공통 형식을 포기하고 상태 코드만으로라도 거부한다.
 */
@Slf4j
final class SecurityErrorResponder {

    private final HandlerExceptionResolver exceptionResolver;

    /**
     * @param exceptionResolver 전역 예외 처리기로 위임하는 MVC 예외 해석기
     */
    SecurityErrorResponder(HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    /**
     * 예외에 맞는 오류 응답을 쓴다. 호출한 쪽은 이후 요청 처리를 이어 가지 않아야 한다.
     *
     * @param request 오류가 난 요청
     * @param response 오류 응답을 쓸 응답
     * @param exception 응답으로 바꿀 예외
     * @throws IOException 상태 코드만으로 거부하는 응답을 쓰지 못한 경우
     */
    void respond(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        if (exceptionResolver.resolveException(request, response, null, exception) != null) {
            return;
        }
        HttpStatus status = fallbackStatus(exception);
        log.error(
                "전역 예외 처리기가 보안 오류 응답을 만들지 못해 상태 코드만 응답합니다: exception={}, status={}",
                exception.getClass().getSimpleName(),
                status.value());
        // 이미 응답이 나가기 시작했으면 상태를 바꿀 수 없다. 이때도 요청 처리는 이어지지 않는다
        if (!response.isCommitted()) {
            response.sendError(status.value());
        }
    }

    // 인증·인가 실패는 원래 나갈 상태(401·403)를 지켜 차량·관리자 클라이언트가 실패 종류를 구분하게 하고,
    // 무엇인지 모르는 예외는 서버 오류로 응답한다
    private HttpStatus fallbackStatus(Exception exception) {
        if (exception instanceof BusinessException businessException) {
            return businessException.getErrorCode().getStatus();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
