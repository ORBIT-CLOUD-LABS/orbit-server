package com.orbit.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AdminPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);

    @Test
    @DisplayName("설정한 관리자 토큰을 읽는다")
    void bindsAdminToken() {
        contextRunner
                .withPropertyValues("orbit.admin.token=secret-token")
                .run(context -> assertThat(
                                context.getBean(AdminProperties.class).token())
                        .isEqualTo("secret-token"));
    }

    @Test
    @DisplayName("관리자 토큰이 없으면 앱을 띄우지 않는다")
    void failsWithoutAdminToken() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("관리자 토큰이 공백이면 앱을 띄우지 않는다")
    void failsWithBlankAdminToken() {
        contextRunner
                .withPropertyValues("orbit.admin.token= ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("문자열로 바꿔도 관리자 토큰을 드러내지 않는다")
    void hidesTokenInToString() {
        assertThat(new AdminProperties("secret-token").toString()).doesNotContain("secret-token");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AdminProperties.class)
    static class PropertiesConfig {}
}
