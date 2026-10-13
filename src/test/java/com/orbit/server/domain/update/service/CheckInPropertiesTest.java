package com.orbit.server.domain.update.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CheckInPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(CheckInConfig.class);

    @Test
    @DisplayName("VEHICLE-REQ-001: 설정한 체크인 주기를 읽는다")
    void bindsInterval() {
        contextRunner
                .withPropertyValues("orbit.check-in.interval-seconds=10")
                .run(context -> assertThat(
                                context.getBean(CheckInProperties.class).intervalSeconds())
                        .isEqualTo(10));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    @DisplayName("VEHICLE-REQ-001: 체크인 주기가 0 이하면 앱을 띄우지 않는다")
    void failsWithNonPositiveInterval(String interval) {
        contextRunner
                .withPropertyValues("orbit.check-in.interval-seconds=" + interval)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 체크인 주기를 설정하지 않으면 앱을 띄우지 않는다")
    void failsWithoutInterval() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }
}
