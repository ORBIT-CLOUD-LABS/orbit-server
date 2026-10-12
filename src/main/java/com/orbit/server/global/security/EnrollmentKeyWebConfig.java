package com.orbit.server.global.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 차량 등록 경로에 등록 키 검사를 건다. (VEHICLE-POL-003) */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EnrollmentProperties.class)
class EnrollmentKeyWebConfig implements WebMvcConfigurer {

    static final String REGISTER_PATH = "/api/v1/vehicles/register";

    private final EnrollmentProperties properties;

    EnrollmentKeyWebConfig(EnrollmentProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new EnrollmentKeyInterceptor(properties)).addPathPatterns(REGISTER_PATH);
    }
}
