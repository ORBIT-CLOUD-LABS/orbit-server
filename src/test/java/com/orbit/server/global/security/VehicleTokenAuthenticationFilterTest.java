package com.orbit.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.ErrorCode;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

class VehicleTokenAuthenticationFilterTest {

    private static final String VEHICLE_ID = "vehicle-001";
    private static final String TOKEN = "vehicle-token";
    private static final String VEHICLE_PATHS = "/api/v1/vehicles/{vehicleId}/**";
    private static final String CHECK_IN_PATH = "/api/v1/vehicles/" + VEHICLE_ID + "/check-in";
    private static final String OTHER_PATH = "/api/v1/admin/artifacts";

    private final VehicleTokenAuthenticator authenticator = mock(VehicleTokenAuthenticator.class);
    private final HandlerExceptionResolver exceptionResolver = mock(HandlerExceptionResolver.class);
    private final VehicleTokenAuthenticationFilter filter = filterOn(VEHICLE_PATHS);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 토큰과 경로 차량을 인증 포트에 넘기고 통과하면 차량 권한으로 인증해 다음 필터로 넘긴다")
    void authenticatesValidToken() throws Exception {
        MockFilterChain chain = doFilter(filter, checkInRequest("Bearer " + TOKEN));

        verify(authenticator).authenticate(TOKEN, VEHICLE_ID);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(VEHICLE_ID);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + VehicleTokenAuthenticationFilter.VEHICLE_ROLE);
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(exceptionResolver);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 토큰이 없으면 UNAUTHORIZED 로 응답하고 다음 필터로 넘기지 않는다")
    void rejectsMissingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", CHECK_IN_PATH);

        MockFilterChain chain = doFilter(filter, request);

        assertRejectedWith(AuthErrorCode.UNAUTHORIZED, request, chain);
        verifyNoInteractions(authenticator);
    }

    @ParameterizedTest
    @ValueSource(strings = {TOKEN, "Basic " + TOKEN})
    @DisplayName("VEHICLE-POL-003: Bearer 형식이 아니면 UNAUTHORIZED 로 응답하고 다음 필터로 넘기지 않는다")
    void rejectsNonBearerToken(String authorization) throws Exception {
        MockHttpServletRequest request = checkInRequest(authorization);

        MockFilterChain chain = doFilter(filter, request);

        assertRejectedWith(AuthErrorCode.UNAUTHORIZED, request, chain);
        verifyNoInteractions(authenticator);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 폐기했거나 발급하지 않은 토큰이면 UNAUTHORIZED 로 응답하고 다음 필터로 넘기지 않는다")
    void rejectsInvalidToken() throws Exception {
        willThrow(new BusinessException(AuthErrorCode.UNAUTHORIZED))
                .given(authenticator)
                .authenticate(TOKEN, VEHICLE_ID);
        MockHttpServletRequest request = checkInRequest("Bearer " + TOKEN);

        MockFilterChain chain = doFilter(filter, request);

        assertRejectedWith(AuthErrorCode.UNAUTHORIZED, request, chain);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 다른 차량의 토큰이면 VEHICLE_MISMATCH 로 응답하고 다음 필터로 넘기지 않는다")
    void rejectsTokenOfOtherVehicle() throws Exception {
        willThrow(new BusinessException(VehicleErrorCode.VEHICLE_MISMATCH))
                .given(authenticator)
                .authenticate(TOKEN, VEHICLE_ID);
        MockHttpServletRequest request = checkInRequest("Bearer " + TOKEN);

        MockFilterChain chain = doFilter(filter, request);

        assertRejectedWith(VehicleErrorCode.VEHICLE_MISMATCH, request, chain);
    }

    @Test
    @DisplayName("경로의 차량 식별자 형식이 틀려 인증 포트가 던진 예외도 전역 예외 처리기로 넘기고 다음 필터로 넘기지 않는다")
    void delegatesInvalidVehicleId() throws Exception {
        InvalidValueException invalidVehicleId = new InvalidValueException("차량 식별자 형식 오류");
        willThrow(invalidVehicleId).given(authenticator).authenticate(TOKEN, VEHICLE_ID);
        MockHttpServletRequest request = checkInRequest("Bearer " + TOKEN);

        MockFilterChain chain = doFilter(filter, request);

        assertThat(resolvedException(request)).isSameAs(invalidVehicleId);
        assertNotAuthenticated(chain);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 차량 토큰 경로가 아니면 토큰을 검사하지 않고 다음 필터로 넘긴다")
    void skipsOtherPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", OTHER_PATH);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);

        MockFilterChain chain = doFilter(filter, request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(authenticator, exceptionResolver);
    }

    @Test
    @DisplayName("경로에 vehicleId 가 없으면 토큰을 확인하지 않고 설정 오류로 처리해 다음 필터로 넘기지 않는다")
    void rejectsPathWithoutVehicleId() throws Exception {
        MockHttpServletRequest request = checkInRequest("Bearer " + TOKEN);

        MockFilterChain chain = doFilter(filterOn("/api/v1/vehicles/**"), request);

        assertThat(resolvedException(request)).isInstanceOf(IllegalStateException.class);
        assertNotAuthenticated(chain);
        verifyNoInteractions(authenticator);
    }

    @Test
    @DisplayName("인증 정보에 토큰 원문을 담지 않는다")
    void doesNotKeepToken() throws Exception {
        doFilter(filter, checkInRequest("Bearer " + TOKEN));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.toString()).doesNotContain(TOKEN);
    }

    private void assertRejectedWith(ErrorCode errorCode, MockHttpServletRequest request, MockFilterChain chain) {
        assertThat(resolvedException(request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(errorCode));
        assertNotAuthenticated(chain);
    }

    private Exception resolvedException(MockHttpServletRequest request) {
        ArgumentCaptor<Exception> exception = ArgumentCaptor.forClass(Exception.class);
        verify(exceptionResolver)
                .resolveException(eq(request), any(HttpServletResponse.class), isNull(), exception.capture());
        return exception.getValue();
    }

    private void assertNotAuthenticated(MockFilterChain chain) {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNull();
    }

    private VehicleTokenAuthenticationFilter filterOn(String pattern) {
        RequestMatcher vehiclePaths = PathPatternRequestMatcher.withDefaults().matcher(pattern);
        return new VehicleTokenAuthenticationFilter(
                vehiclePaths, authenticator, new SecurityErrorResponder(exceptionResolver));
    }

    private MockHttpServletRequest checkInRequest(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", CHECK_IN_PATH);
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        return request;
    }

    private MockFilterChain doFilter(VehicleTokenAuthenticationFilter target, MockHttpServletRequest request)
            throws Exception {
        MockFilterChain chain = new MockFilterChain();
        target.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }
}
