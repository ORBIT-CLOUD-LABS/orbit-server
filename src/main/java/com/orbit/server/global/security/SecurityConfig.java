package com.orbit.server.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 요청 인증의 공통 기반인 Spring Security 필터 체인을 구성한다.
 *
 * <p>모든 호출 주체(차량·관리자)가 요청마다 {@code Authorization: Bearer} 토큰을 보내는 API 서버이므로 세션·CSRF·폼 로그인·HTTP
 * Basic 을 쓰지 않는다.
 *
 * <p>{@code @WebMvcTest} 는 이 설정을 자동으로 올리지 않으므로 Spring Boot 기본 보안 설정(HTTP Basic)이 대신 걸린다. 슬라이스
 * 테스트에서는 {@code @Import} 로 넣는다.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 차량 인증은 MVC 인터셉터가 맡고 있으므로 경로별 규칙을 정하기 전까지는 필터 단계에서 막지 않는다
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .build();
    }
}
