package com.orbit.server.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer {admin-token}} 이 설정한 관리자 토큰과 같으면 요청을 관리자로 인증한다.
 *
 * <p>토큰이 없거나 다르면 거부하지 않고 인증 없이 다음 필터로 넘긴다. 같은 헤더로 차량 토큰·등록 키도 오기 때문이며, 거부할지는
 * 경로별 인가 규칙이 정한다.
 */
class AdminTokenAuthenticationFilter extends OncePerRequestFilter {

    static final String ADMIN_ROLE = "ADMIN";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PRINCIPAL = "admin";

    private final byte[] adminToken;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    /**
     * @param properties 관리자 토큰 설정
     */
    AdminTokenAuthenticationFilter(AdminProperties properties) {
        this.adminToken = properties.token().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isAdmin(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            authenticateAsAdmin();
        }
        chain.doFilter(request, response);
    }

    private boolean isAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return false;
        }
        byte[] presented = authorization.substring(BEARER_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        // 응답 시간 차이로 토큰을 한 글자씩 맞혀 볼 수 없도록 일치하는 길이와 상관없이 같은 시간에 비교한다
        return MessageDigest.isEqual(presented, adminToken);
    }

    // 인증 정보에 토큰 원문을 담지 않아 SecurityContext 가 로그·디버그 출력에 찍혀도 토큰이 남지 않는다
    private void authenticateAsAdmin() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                ADMIN_PRINCIPAL, null, AuthorityUtils.createAuthorityList("ROLE_" + ADMIN_ROLE));
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
    }
}
