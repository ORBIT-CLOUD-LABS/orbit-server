package com.orbit.server.global.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 요청 인증의 공통 기반인 Spring Security 필터 체인을 구성하고 경로별 인증 규칙을 정한다.
 *
 * <p>모든 호출 주체(차량·관리자)가 요청마다 {@code Authorization: Bearer} 토큰을 보내는 API 서버이므로 세션·CSRF·폼 로그인·HTTP
 * Basic 을 쓰지 않는다.
 *
 * <p>{@code @WebMvcTest} 는 이 설정을 자동으로 올리지 않으므로 Spring Boot 기본 보안 설정(HTTP Basic)이 대신 걸린다. 슬라이스
 * 테스트에서는 {@code @Import} 로 넣는다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminProperties.class)
class SecurityConfig {

    static final String HEALTH_PATHS = "/actuator/health/**";
    static final String VEHICLE_PATHS = "/api/v1/vehicles/**";
    static final String ADMIN_PATHS = "/api/v1/admin/**";
    static final String DASHBOARD_PATHS = "/api/v1/dashboard/**";

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AdminProperties adminProperties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver)
            throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        new AdminTokenAuthenticationFilter(adminProperties), AnonymousAuthenticationFilter.class)
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(new ErrorResponseAuthenticationEntryPoint(exceptionResolver)))
                .authorizeHttpRequests(requests -> requests
                        // 이미 인가를 통과한 요청의 오류 응답을 만드는 내부 재요청이므로 다시 막지 않는다
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(HEALTH_PATHS)
                        .permitAll()
                        // 차량 API 는 등록 키·차량 토큰 인터셉터가 인증하므로 관리자 인증 대상에서 뺀다
                        .requestMatchers(VEHICLE_PATHS)
                        .permitAll()
                        .requestMatchers(ADMIN_PATHS, DASHBOARD_PATHS)
                        .hasRole(AdminTokenAuthenticationFilter.ADMIN_ROLE)
                        // 새 API 가 규칙 없이 열리지 않도록 규칙에 없는 경로는 거부한다
                        .anyRequest()
                        .denyAll())
                .build();
    }
}
