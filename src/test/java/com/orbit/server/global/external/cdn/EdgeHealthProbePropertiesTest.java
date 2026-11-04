package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EdgeHealthProbePropertiesTest {

    private static final String TIMEOUT = "orbit.cdn.health-check.timeout=";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(EdgeHealthProbeConfig.class);

    @Test
    @DisplayName("설정한 헬스체크 호출 타임아웃을 읽는다")
    void bindsTimeout() {
        contextRunner
                .withPropertyValues(TIMEOUT + "2s")
                .run(context -> assertThat(
                                context.getBean(EdgeHealthProbeProperties.class).timeout())
                        .isEqualTo(Duration.ofSeconds(2)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s"})
    @DisplayName("타임아웃이 0 이하면 앱을 띄우지 않는다")
    void failsWithNonPositiveTimeout(String timeout) {
        contextRunner
                .withPropertyValues(TIMEOUT + timeout)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("타임아웃이 없으면 앱을 띄우지 않는다")
    void failsWithoutTimeout() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }
}
