package com.orbit.server.global.security.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class RoleAuthenticationTest {

    private static final String PRINCIPAL = "vehicle-001";
    private static final String ROLE = "VEHICLE";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("주체 이름과 ROLE_ 접두사를 붙인 권한 하나로 인증된 상태를 만든다")
    void establishesAuthenticatedPrincipalWithRole() {
        RoleAuthentication.establish(PRINCIPAL, ROLE);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(PRINCIPAL);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + ROLE);
    }

    @Test
    @DisplayName("인증 정보에 자격 증명을 담지 않는다")
    void keepsNoCredentials() {
        RoleAuthentication.establish(PRINCIPAL, ROLE);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials())
                .isNull();
    }

    @Test
    @DisplayName("이미 있던 인증 정보를 대체한다")
    void replacesExistingAuthentication() {
        RoleAuthentication.establish("admin", "ADMIN");

        RoleAuthentication.establish(PRINCIPAL, ROLE);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getName()).isEqualTo(PRINCIPAL);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + ROLE);
    }
}
