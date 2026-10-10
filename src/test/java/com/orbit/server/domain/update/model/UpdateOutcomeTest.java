package com.orbit.server.domain.update.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateOutcomeTest {

    private static final Instant FINISHED_AT = Instant.parse("2026-10-01T12:00:00Z");
    private static final UpdateFailure FAILURE = new UpdateFailure(FailureReason.HASH_MISMATCH, null);

    @Test
    @DisplayName("UPDATE-REQ-001: 성공 결과는 실패 정보 없이 생성한다")
    void createsSucceededWithoutFailure() {
        assertThatCode(() -> new UpdateOutcome(UpdateStatus.SUCCEEDED, null, FINISHED_AT))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 실패 결과는 실패 정보와 함께 생성한다")
    void createsFailedWithFailure() {
        assertThatCode(() -> new UpdateOutcome(UpdateStatus.FAILED, FAILURE, FINISHED_AT))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 실패 결과에 실패 정보가 없으면 거부한다")
    void rejectsFailedWithoutFailure() {
        assertThatThrownBy(() -> new UpdateOutcome(UpdateStatus.FAILED, null, FINISHED_AT))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 성공 결과에 실패 정보가 있으면 거부한다")
    void rejectsSucceededWithFailure() {
        assertThatThrownBy(() -> new UpdateOutcome(UpdateStatus.SUCCEEDED, FAILURE, FINISHED_AT))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 완료 시각이 없으면 거부한다")
    void rejectsMissingFinishedAt() {
        assertThatThrownBy(() -> new UpdateOutcome(UpdateStatus.SUCCEEDED, null, null))
                .isInstanceOf(InvalidValueException.class);
    }
}
