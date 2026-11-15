package com.orbit.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class SecurityErrorResponderTest {

    private final HandlerExceptionResolver exceptionResolver = mock(HandlerExceptionResolver.class);
    private final SecurityErrorResponder responder = new SecurityErrorResponder(exceptionResolver);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    @DisplayName("예외를 전역 예외 처리기로 넘기고, 처리기가 응답하면 응답에 더 손대지 않는다")
    void delegatesToExceptionResolver() throws Exception {
        BusinessException exception = new BusinessException(AuthErrorCode.UNAUTHORIZED);
        given(exceptionResolver.resolveException(request, response, null, exception))
                .willReturn(new ModelAndView());

        responder.respond(request, response, exception);

        verify(exceptionResolver).resolveException(request, response, null, exception);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.isCommitted()).isFalse();
    }

    @Test
    @DisplayName("처리기가 응답하지 못하면 인증 실패는 오류 코드의 상태 401 로 거부한다")
    void rejectsWithErrorCodeStatusWhenUnresolved() throws Exception {
        responder.respond(request, response, new BusinessException(AuthErrorCode.UNAUTHORIZED));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("처리기가 응답하지 못하면 다른 차량의 토큰은 오류 코드의 상태 403 으로 거부한다")
    void rejectsWithForbiddenWhenUnresolved() throws Exception {
        responder.respond(request, response, new BusinessException(VehicleErrorCode.VEHICLE_MISMATCH));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("처리기가 응답하지 못하면 오류 코드가 없는 예외는 500 으로 거부한다")
    void rejectsWithServerErrorWhenUnresolved() throws Exception {
        responder.respond(request, response, new InvalidValueException("차량 식별자 형식 오류"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("처리기가 응답하지 못했는데 응답이 이미 나가기 시작했으면 상태를 바꾸려 하지 않는다")
    void leavesCommittedResponse() throws Exception {
        response.setStatus(HttpStatus.ACCEPTED.value());
        response.setCommitted(true);

        responder.respond(request, response, new BusinessException(AuthErrorCode.UNAUTHORIZED));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
    }
}
