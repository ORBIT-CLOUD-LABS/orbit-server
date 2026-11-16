package com.orbit.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.orbit.server.global.error.BusinessException;
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
import org.springframework.web.servlet.HandlerExceptionResolver;

class EnrollmentKeyAuthenticationFilterTest {

    private static final String ENROLLMENT_KEY = "test-enrollment-key";
    private static final String REGISTER_PATH = "/api/v1/vehicles/register";
    private static final String OTHER_PATH = "/api/v1/vehicles/vehicle-001/check-in";

    private final HandlerExceptionResolver exceptionResolver = mock(HandlerExceptionResolver.class);
    private final EnrollmentKeyAuthenticationFilter filter = new EnrollmentKeyAuthenticationFilter(
            PathPatternRequestMatcher.withDefaults().matcher(REGISTER_PATH),
            new EnrollmentProperties(ENROLLMENT_KEY),
            new SecurityErrorResponder(exceptionResolver));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 등록 키가 맞으면 등록 권한으로 인증하고 다음 필터로 넘긴다")
    void authenticatesValidKey() throws Exception {
        MockFilterChain chain = doFilter(registerRequest("Bearer " + ENROLLMENT_KEY));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + EnrollmentKeyAuthenticationFilter.ENROLLMENT_ROLE);
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(exceptionResolver);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-key", "Bearer ", ENROLLMENT_KEY, "Basic " + ENROLLMENT_KEY})
    @DisplayName("VEHICLE-POL-003: 등록 키가 다르거나 Bearer 형식이 아니면 INVALID_ENROLLMENT_KEY 로 응답하고 다음 필터로 넘기지 않는다")
    void rejectsInvalidKey(String authorization) throws Exception {
        MockHttpServletRequest request = registerRequest(authorization);

        MockFilterChain chain = doFilter(request);

        assertRejected(request, chain);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 등록 키가 없으면 INVALID_ENROLLMENT_KEY 로 응답하고 다음 필터로 넘기지 않는다")
    void rejectsMissingKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", REGISTER_PATH);

        MockFilterChain chain = doFilter(request);

        assertRejected(request, chain);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 등록 경로가 아니면 등록 키를 검사하지 않고 다음 필터로 넘긴다")
    void skipsOtherPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", OTHER_PATH);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ENROLLMENT_KEY);

        MockFilterChain chain = doFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(exceptionResolver);
    }

    @Test
    @DisplayName("인증 정보에 등록 키 원문을 담지 않는다")
    void doesNotKeepKey() throws Exception {
        doFilter(registerRequest("Bearer " + ENROLLMENT_KEY));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.toString()).doesNotContain(ENROLLMENT_KEY);
    }

    private void assertRejected(MockHttpServletRequest request, MockFilterChain chain) {
        ArgumentCaptor<Exception> exception = ArgumentCaptor.forClass(Exception.class);
        verify(exceptionResolver)
                .resolveException(eq(request), any(HttpServletResponse.class), isNull(), exception.capture());
        assertThat(exception.getValue())
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_ENROLLMENT_KEY));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNull();
    }

    private MockHttpServletRequest registerRequest(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", REGISTER_PATH);
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        return request;
    }

    private MockFilterChain doFilter(MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }
}
