package com.orbit.server.domain.edge.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EdgeHealthCheckPropertiesTest {

    private static final String INTERVAL = "orbit.cdn.health-check.interval=";
    private static final String FAILURE_THRESHOLD = "orbit.cdn.health-check.failure-threshold=";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EdgeHealthCheckConfig.class)
            .withPropertyValues(INTERVAL + "10s", FAILURE_THRESHOLD + "3");

    @Test
    @DisplayName("설정한 헬스체크 주기와 실패 기준 횟수를 읽는다")
    void bindsIntervalAndThreshold() {
        contextRunner.run(context -> assertThat(context.getBean(EdgeHealthCheckProperties.class))
                .isEqualTo(new EdgeHealthCheckProperties(Duration.ofSeconds(10), 3)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s"})
    @DisplayName("헬스체크 주기가 0 이하면 앱을 띄우지 않는다")
    void failsWithNonPositiveInterval(String interval) {
        contextRunner
                .withPropertyValues(INTERVAL + interval)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("실패 기준 횟수가 1 보다 작으면 앱을 띄우지 않는다")
    void failsWithThresholdBelowOne() {
        contextRunner
                .withPropertyValues(FAILURE_THRESHOLD + "0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("설정이 없으면 앱을 띄우지 않는다")
    void failsWithoutSettings() {
        new ApplicationContextRunner()
                .withUserConfiguration(EdgeHealthCheckConfig.class)
                .run(context -> assertThat(context).hasFailed());
    }
}
