package com.orbit.server.global.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.http.HttpHeaders;

/**
 * 요청의 {@code Authorization: Bearer {값}} 헤더에서 값을 꺼낸다.
 *
 * <p>등록 키·차량 토큰·관리자 토큰이 모두 같은 헤더 형식으로 오므로, 형식 해석을 인증 수단별 필터마다 따로 두지 않고 여기서 한다. 꺼낸
 * 값이 무엇이고 유효한지는 판단하지 않는다.
 */
final class BearerTokenExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    private BearerTokenExtractor() {}

    /**
     * @param request 인증 헤더를 읽을 요청
     * @return {@code Bearer } 뒤의 값. 헤더가 없거나 Bearer 형식이 아니면 비어 있다. {@code Bearer } 뒤가 비어 있으면 빈
     *     문자열이며, 거부할지는 값을 확인하는 쪽이 정한다
     */
    static Optional<String> extract(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(authorization.substring(BEARER_PREFIX.length()));
    }
}
