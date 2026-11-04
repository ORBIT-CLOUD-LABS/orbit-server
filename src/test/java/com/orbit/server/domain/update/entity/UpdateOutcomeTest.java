package com.orbit.server.domain.update.entity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UpdateOutcomeTest {

    private static final Instant FINISHED_AT = Instant.parse("2026-10-01T12:00:00Z");
    private static final UpdateFailure FAILURE = new UpdateFailure(FailureReason.HASH_MISMATCH, null);
    private static final String EDGE_ID = "edge-seoul";

    @Test
    @DisplayName("UPDATE-REQ-001: 성공 결과는 실패 정보 없이 생성한다")
    void createsSucceededWithoutFailure() {
        assertThatCode(() -> new UpdateOutcome(UpdateStatus.SUCCEEDED, null, FINISHED_AT, EDGE_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 실패 결과는 실패 정보와 함께 생성한다")
    void createsFailedWithFailure() {
        assertThatCode(() -> new UpdateOutcome(UpdateStatus.FAILED, FAILURE, FINISHED_AT, EDGE_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 실패 결과에 실패 정보가 없으면 거부한다")
    void rejectsFailedWithoutFailure() {
        assertThatThrownBy(() -> new UpdateOutcome(UpdateStatus.FAILED, null, FINISHED_AT, EDGE_ID))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 성공 결과에 실패 정보가 있으면 거부한다")
    void rejectsSucceededWithFailure() {
        assertThatThrownBy(() -> new UpdateOutcome(UpdateStatus.SUCCEEDED, FAILURE, FINISHED_AT, EDGE_ID))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 완료 시각이 없으면 거부한다")
    void rejectsMissingFinishedAt() {
        assertThatThrownBy(() -> new UpdateOutcome(UpdateStatus.SUCCEEDED, null, null, EDGE_ID))
                .isInstanceOf(InvalidValueException.class);
    }

    // 다운로드 전에 실패했거나, 아직 Edge 를 보내지 않는 차량의 성공 결과도 받는다
    @Test
    @DisplayName("UPDATE-REQ-001: 다운로드 Edge 없이도 생성한다")
    void createsWithoutDownloadEdge() {
        assertThatCode(() -> {
                    new UpdateOutcome(UpdateStatus.FAILED, FAILURE, FINISHED_AT, null);
                    new UpdateOutcome(UpdateStatus.SUCCEEDED, null, FINISHED_AT, null);
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("UPDATE-REQ-001: 다운로드 Edge 가 공백이면 거부한다")
    void rejectsBlankDownloadEdge(String edgeId) {
        assertThatThrownBy(() -> new UpdateOutcome(UpdateStatus.SUCCEEDED, null, FINISHED_AT, edgeId))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 다운로드 Edge 가 컬럼 길이(50자)를 넘으면 거부한다")
    void rejectsTooLongDownloadEdge() {
        String edgeId = "e".repeat(51);

        assertThatThrownBy(() -> new UpdateOutcome(UpdateStatus.SUCCEEDED, null, FINISHED_AT, edgeId))
                .isInstanceOf(InvalidValueException.class);
    }
}
