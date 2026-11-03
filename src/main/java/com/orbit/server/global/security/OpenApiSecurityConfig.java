package com.orbit.server.global.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Optional;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * API 문서에 경로별 인증 방식을 표시한다.
 *
 * <p>세 인증 모두 {@code Authorization: Bearer} 헤더를 쓰지만 넣는 값과 적용 경로가 달라 스키마를 나눈다. 경로 기준은 실제 인증을 거는
 * {@link SecurityConfig}, {@link VehicleTokenWebConfig}, {@link EnrollmentKeyWebConfig} 의 상수를 그대로 써서 문서와
 * 동작이 어긋나지 않게 한다. 이 설정은 문서에만 영향을 주고 인증 동작은 바꾸지 않는다.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiSecurityConfig {

    static final String ADMIN_TOKEN = "adminToken";
    static final String VEHICLE_TOKEN = "vehicleToken";
    static final String ENROLLMENT_KEY = "enrollmentKey";

    private static final String BEARER = "bearer";

    // 위에서부터 처음 맞는 규칙을 쓴다. 등록 경로는 차량 경로 패턴에도 맞으므로 먼저 둔다
    private static final List<SchemeRule> RULES = List.of(
            SchemeRule.of(EnrollmentKeyWebConfig.REGISTER_PATH, ENROLLMENT_KEY),
            SchemeRule.of(VehicleTokenWebConfig.VEHICLE_PATHS, VEHICLE_TOKEN),
            SchemeRule.of(SecurityConfig.ADMIN_PATHS, ADMIN_TOKEN),
            SchemeRule.of(SecurityConfig.DASHBOARD_PATHS, ADMIN_TOKEN));

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("ORBIT OTA Server API").version("v1"))
                .components(new Components()
                        .addSecuritySchemes(ADMIN_TOKEN, bearer("관리자 토큰. 관리자·대시보드 API"))
                        .addSecuritySchemes(VEHICLE_TOKEN, bearer("차량 등록 응답의 vehicleToken. 체크인·매니페스트 API"))
                        .addSecuritySchemes(ENROLLMENT_KEY, bearer("차량 등록 키. 차량 등록 API")));
    }

    @Bean
    OpenApiCustomizer securityRequirementCustomizer() {
        return openApi -> openApi.getPaths().forEach(OpenApiSecurityConfig::requireScheme);
    }

    private static SecurityScheme bearer(String description) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme(BEARER)
                .description(description);
    }

    private static void requireScheme(String path, PathItem pathItem) {
        schemeOf(path)
                .ifPresent(scheme -> pathItem.readOperations()
                        .forEach(operation -> operation.addSecurityItem(new SecurityRequirement().addList(scheme))));
    }

    private static Optional<String> schemeOf(String path) {
        PathContainer container = PathContainer.parsePath(path);
        return RULES.stream()
                .filter(rule -> rule.matches(container))
                .map(SchemeRule::scheme)
                .findFirst();
    }

    private record SchemeRule(PathPattern pattern, String scheme) {

        static SchemeRule of(String pattern, String scheme) {
            return new SchemeRule(PathPatternParser.defaultInstance.parse(pattern), scheme);
        }

        boolean matches(PathContainer path) {
            return pattern.matches(path);
        }
    }
}
