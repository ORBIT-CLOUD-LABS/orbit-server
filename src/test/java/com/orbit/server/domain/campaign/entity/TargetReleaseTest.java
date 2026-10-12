package com.orbit.server.domain.campaign.entity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TargetReleaseTest {

    private static final TargetRelease TARGET = new TargetRelease("model-a", "1.0.1");

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 파일의 차종·버전과 같으면 통과한다")
    void passesWhenSameAsArtifact() {
        assertThatCode(() -> TARGET.requireSameAs(new TargetRelease("model-a", "1.0.1")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 목표 버전이 파일 버전과 다르면 거부한다")
    void rejectsDifferentVersion() {
        assertThatThrownBy(() -> TARGET.requireSameAs(new TargetRelease("model-a", "1.0.0")))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 대상 차종이 파일 차종과 다르면 거부한다")
    void rejectsDifferentModel() {
        assertThatThrownBy(() -> TARGET.requireSameAs(new TargetRelease("model-b", "1.0.1")))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 목표 버전이 없으면 거부한다")
    void rejectsMissingVersion() {
        assertThatThrownBy(() -> new TargetRelease("model-a", " ")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 목표 버전이 50자를 넘으면 거부한다")
    void rejectsTooLongVersion() {
        assertThatThrownBy(() -> new TargetRelease("model-a", "1".repeat(51)))
                .isInstanceOf(InvalidValueException.class);
    }
}
