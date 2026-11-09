package com.orbit.server.domain.edge.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EdgeSelectionPropertiesTest {

    private static final String FALLBACK_COUNT = "orbit.cdn.fallback-count=";
    private static final String LOCATION_MAX_AGE = "orbit.cdn.location-max-age=";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EdgeSelectionConfig.class)
            .withPropertyValues(FALLBACK_COUNT + "2", LOCATION_MAX_AGE + "10m");

    @Test
    @DisplayName("MANIFEST-POL-001: fallback 개수와 위치 유효 시간을 읽는다")
    void bindsFallbackCountAndLocationMaxAge() {
        contextRunner.run(context -> assertThat(context.getBean(EdgeSelectionProperties.class))
                .isEqualTo(new EdgeSelectionProperties(2, Duration.ofMinutes(10))));
    }

    @Test
    @DisplayName("fallback 개수가 음수면 앱을 띄우지 않는다")
    void failsWithNegativeFallbackCount() {
        contextRunner
                .withPropertyValues(FALLBACK_COUNT + "-1")
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s"})
    @DisplayName("위치 유효 시간이 0 이하면 앱을 띄우지 않는다")
    void failsWithNonPositiveLocationMaxAge(String maxAge) {
        contextRunner
                .withPropertyValues(LOCATION_MAX_AGE + maxAge)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("설정이 없으면 앱을 띄우지 않는다")
    void failsWithoutSettings() {
        new ApplicationContextRunner()
                .withUserConfiguration(EdgeSelectionConfig.class)
                .run(context -> assertThat(context).hasFailed());
    }
}
