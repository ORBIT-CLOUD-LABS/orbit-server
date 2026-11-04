package com.orbit.server.domain.edge.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EdgeHealthTest {

    private static final int THRESHOLD = 3;

    @Test
    @DisplayName("첫 확인 전에는 정상으로 본다")
    void startsHealthy() {
        assertThat(EdgeHealth.initial().isHealthy(THRESHOLD)).isTrue();
    }

    @Test
    @DisplayName("연속 실패가 기준 횟수보다 적으면 정상으로 본다")
    void staysHealthyBelowThreshold() {
        EdgeHealth health = EdgeHealth.initial().failure().failure();

        assertThat(health.isHealthy(THRESHOLD)).isTrue();
    }

    @Test
    @DisplayName("연속 실패가 기준 횟수에 이르면 비정상으로 본다")
    void becomesUnhealthyAtThreshold() {
        EdgeHealth health = EdgeHealth.initial().failure().failure().failure();

        assertThat(health.isHealthy(THRESHOLD)).isFalse();
    }

    @Test
    @DisplayName("한 번 성공하면 연속 실패가 끊겨 다시 정상으로 본다")
    void recoversAfterOneSuccess() {
        EdgeHealth health = EdgeHealth.initial().failure().failure().failure().success();

        assertThat(health.isHealthy(THRESHOLD)).isTrue();
    }

    @Test
    @DisplayName("성공 사이의 실패는 이어서 세지 않는다")
    void resetsFailuresOnSuccess() {
        EdgeHealth health =
                EdgeHealth.initial().failure().failure().success().failure().failure();

        assertThat(health.isHealthy(THRESHOLD)).isTrue();
    }

    @Test
    @DisplayName("연속 실패 횟수가 음수면 거부한다")
    void rejectsNegativeFailures() {
        assertThatThrownBy(() -> new EdgeHealth(-1)).isInstanceOf(InvalidValueException.class);
    }
}
