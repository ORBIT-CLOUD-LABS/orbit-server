package com.orbit.server.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer {admin-token}} 이 설정한 관리자 토큰과 같으면 요청을 관리자로 인증한다.
 *
 * <p>토큰이 없거나 다르면 거부하지 않고 인증 없이 다음 필터로 넘긴다. 같은 헤더로 차량 토큰·등록 키도 오기 때문이며, 거부할지는
 * 경로별 인가 규칙이 정한다.
 */
class AdminTokenAuthenticationFilter extends OncePerRequestFilter {

    static final String ADMIN_ROLE = "ADMIN";

    private static final String ADMIN_PRINCIPAL = "admin";

    private final byte[] adminToken;

    /**
     * @param properties 관리자 토큰 설정
     */
    AdminTokenAuthenticationFilter(AdminProperties properties) {
        this.adminToken = properties.token().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (BearerTokenExtractor.extract(request).filter(this::isAdminToken).isPresent()) {
            RoleAuthentication.establish(ADMIN_PRINCIPAL, ADMIN_ROLE);
        }
        chain.doFilter(request, response);
    }

    // 응답 시간 차이로 토큰을 한 글자씩 맞혀 볼 수 없도록 일치하는 길이와 상관없이 같은 시간에 비교한다
    private boolean isAdminToken(String presented) {
        return MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), adminToken);
    }
}
