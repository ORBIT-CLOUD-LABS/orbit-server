package com.orbit.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminTokenAuthenticationFilterTest {

    private static final String ADMIN_TOKEN = "test-admin-token";

    private final AdminTokenAuthenticationFilter filter =
            new AdminTokenAuthenticationFilter(new AdminProperties(ADMIN_TOKEN));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("관리자 토큰이 맞으면 관리자로 인증하고 다음 필터로 넘긴다")
    void authenticatesValidToken() throws Exception {
        MockFilterChain chain = doFilter("Bearer " + ADMIN_TOKEN);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + AdminTokenAuthenticationFilter.ADMIN_ROLE);
        assertThat(chain.getRequest()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Bearer ", ADMIN_TOKEN, "Basic " + ADMIN_TOKEN})
    @DisplayName("관리자 토큰이 다르거나 Bearer 형식이 아니면 인증하지 않고 다음 필터로 넘긴다")
    void skipsInvalidToken(String authorization) throws Exception {
        MockFilterChain chain = doFilter(authorization);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("토큰이 없으면 인증하지 않고 다음 필터로 넘긴다")
    void skipsMissingToken() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("인증 정보에 토큰 원문을 담지 않는다")
    void doesNotKeepToken() throws Exception {
        doFilter("Bearer " + ADMIN_TOKEN);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.toString()).doesNotContain(ADMIN_TOKEN);
    }

    private MockFilterChain doFilter(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }
}
