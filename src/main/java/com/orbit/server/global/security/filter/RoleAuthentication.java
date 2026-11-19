package com.orbit.server.global.security.filter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * 토큰·키 확인을 마친 요청을 역할 하나를 가진 주체로 인증된 상태로 만든다.
 *
 * <p>인증 수단(등록 키, 차량 토큰, 관리자 토큰)마다 확인 방법은 다르지만 확인 뒤에 남기는 인증 정보의 형태는 같으므로 여기서 만든다.
 * 인증 정보에 토큰·키 원문을 받지 않아, {@code SecurityContext} 가 로그·디버그 출력에 찍혀도 원문이 남지 않는다.
 */
final class RoleAuthentication {

    private static final String ROLE_PREFIX = "ROLE_";

    private RoleAuthentication() {}

    /**
     * 현재 요청의 {@code SecurityContext} 에 인증 정보를 넣는다. 이미 있던 인증 정보는 대체한다.
     *
     * @param principal 인증된 주체의 이름. 토큰·키 원문을 넘기지 않는다
     * @param role {@code ROLE_} 접두사를 뺀 역할 이름. 인가 규칙의 {@code hasRole} 에 쓰는 값과 같다
     */
    static void establish(String principal, String role) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, AuthorityUtils.createAuthorityList(ROLE_PREFIX + role));
        SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
    }
}
