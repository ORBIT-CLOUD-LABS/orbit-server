package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DashboardStreamPropertiesTest {

    private static final String POLL_INTERVAL = "orbit.dashboard.stream.poll-interval=";
    private static final String KEEP_ALIVE_INTERVAL = "orbit.dashboard.stream.keep-alive-interval=";
    private static final String OVERLAP = "orbit.dashboard.stream.overlap=";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(DashboardStreamConfig.class);

    @Test
    @DisplayName("MONITORING-REQ-001: 설정한 조회 주기·keep-alive 간격·겹침 시간을 읽는다")
    void bindsDurations() {
        contextRunner
                .withPropertyValues(POLL_INTERVAL + "1s", KEEP_ALIVE_INTERVAL + "15s", OVERLAP + "10s")
                .run(context -> assertThat(context.getBean(DashboardStreamProperties.class))
                        .isEqualTo(new DashboardStreamProperties(
                                Duration.ofSeconds(1), Duration.ofSeconds(15), Duration.ofSeconds(10))));
    }

    @ParameterizedTest
    @ValueSource(strings = {POLL_INTERVAL, KEEP_ALIVE_INTERVAL, OVERLAP})
    @DisplayName("MONITORING-REQ-001: 간격이 0 이면 앱을 띄우지 않는다")
    void failsWithZeroDuration(String property) {
        contextRunner
                .withPropertyValues(POLL_INTERVAL + "1s", KEEP_ALIVE_INTERVAL + "15s", OVERLAP + "10s")
                .withPropertyValues(property + "0s")
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {POLL_INTERVAL, KEEP_ALIVE_INTERVAL, OVERLAP})
    @DisplayName("MONITORING-REQ-001: 간격이 음수면 앱을 띄우지 않는다")
    void failsWithNegativeDuration(String property) {
        contextRunner
                .withPropertyValues(POLL_INTERVAL + "1s", KEEP_ALIVE_INTERVAL + "15s", OVERLAP + "10s")
                .withPropertyValues(property + "-1s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 간격을 설정하지 않으면 앱을 띄우지 않는다")
    void failsWithoutDurations() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }
}
