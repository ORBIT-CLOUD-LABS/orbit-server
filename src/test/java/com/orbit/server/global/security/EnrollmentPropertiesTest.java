package com.orbit.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class EnrollmentPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);

    @Test
    @DisplayName("VEHICLE-POL-003: 설정한 등록 키를 읽는다")
    void bindsEnrollmentKey() {
        contextRunner
                .withPropertyValues("orbit.enrollment.key=secret-key")
                .run(context -> assertThat(
                                context.getBean(EnrollmentProperties.class).key())
                        .isEqualTo("secret-key"));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 등록 키가 없으면 앱을 띄우지 않는다")
    void failsWithoutEnrollmentKey() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 등록 키가 공백이면 앱을 띄우지 않는다")
    void failsWithBlankEnrollmentKey() {
        contextRunner
                .withPropertyValues("orbit.enrollment.key= ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EnrollmentProperties.class)
    static class PropertiesConfig {}
}
