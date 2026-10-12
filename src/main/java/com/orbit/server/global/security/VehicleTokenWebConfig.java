package com.orbit.server.global.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 등록을 뺀 차량 API 에 차량 토큰 검사를 건다. 등록은 공통 등록 키로 따로 검사한다. (VEHICLE-POL-003) */
@Configuration(proxyBeanMethods = false)
class VehicleTokenWebConfig implements WebMvcConfigurer {

    static final String VEHICLE_PATHS = "/api/v1/vehicles/**";

    private final VehicleTokenAuthenticator authenticator;

    VehicleTokenWebConfig(VehicleTokenAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new VehicleTokenInterceptor(authenticator))
                .addPathPatterns(VEHICLE_PATHS)
                .excludePathPatterns(EnrollmentKeyWebConfig.REGISTER_PATH);
    }
}
