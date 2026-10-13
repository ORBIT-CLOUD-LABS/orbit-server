package com.orbit.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.global.error.CommonErrorCode;
import com.orbit.server.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerExceptionResolver;

// 전역 예외 처리기를 거친 실제 응답을 확인하려고 MVC 예외 해석기가 올라오는 슬라이스에서 검증한다
@WebMvcTest(
        controllers = ErrorResponseAuthenticationEntryPointTest.ProbeController.class,
        properties = "orbit.enrollment.key=test-enrollment-key")
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ErrorResponseAuthenticationEntryPointTest {

    private static final String TOKEN = "secret-admin-token";

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver exceptionResolver;

    // @WebMvcTest 가 차량 토큰 인터셉터 설정도 올리므로 포트 구현 대신 넣는다
    @MockitoBean
    private VehicleTokenAuthenticator vehicleTokenAuthenticator;

    @Test
    @DisplayName("인증이 없으면 401 AUTHENTICATION_REQUIRED 를 공통 오류 형식으로 응답한다")
    void respondsAuthenticationRequired() throws Exception {
        MockHttpServletResponse response = commence(new MockHttpServletRequest());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString())
                .contains("\"code\":\"" + CommonErrorCode.AUTHENTICATION_REQUIRED.getCode() + "\"")
                .contains("\"message\":\"" + CommonErrorCode.AUTHENTICATION_REQUIRED.getMessage() + "\"");
    }

    @Test
    @DisplayName("요청한 토큰과 인증 예외 메시지를 응답에 옮기지 않는다")
    void doesNotExposeToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);

        MockHttpServletResponse response = commence(request, new BadCredentialsException(TOKEN));

        assertThat(response.getContentAsString()).doesNotContain(TOKEN);
    }

    private MockHttpServletResponse commence(MockHttpServletRequest request) throws Exception {
        return commence(request, new BadCredentialsException("인증 실패"));
    }

    private MockHttpServletResponse commence(MockHttpServletRequest request, BadCredentialsException exception)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ErrorResponseAuthenticationEntryPoint(exceptionResolver).commence(request, response, exception);
        return response;
    }

    // 다른 컨트롤러와 그 의존성을 올리지 않도록 슬라이스 범위를 좁히는 용도
    @RestController
    static class ProbeController {}
}
