package com.orbit.server.domain.update.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.domain.update.entity.FailureReason;
import com.orbit.server.domain.update.entity.UpdateFailure;
import com.orbit.server.domain.update.entity.UpdateOutcome;
import com.orbit.server.domain.update.entity.UpdateStatus;
import com.orbit.server.global.error.InvalidValueException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ReportUpdateResultCommandTest {

    private static final String FINISHED_AT = "2026-10-01T12:00:00Z";
    private static final Instant FINISHED_INSTANT = Instant.parse(FINISHED_AT);

    @Test
    @DisplayName("UPDATE-REQ-001: 성공 보고를 실패 정보 없는 결과로 바꾼다")
    void convertsSucceeded() {
        assertThat(reportOf("SUCCEEDED", null, null).toOutcome())
                .isEqualTo(new UpdateOutcome(UpdateStatus.SUCCEEDED, null, FINISHED_INSTANT));
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 실패 보고를 실패 사유·상세가 있는 결과로 바꾼다")
    void convertsFailed() {
        UpdateFailure failure = new UpdateFailure(FailureReason.HASH_MISMATCH, "sha256 mismatch");

        assertThat(reportOf("FAILED", "HASH_MISMATCH", "sha256 mismatch").toOutcome())
                .isEqualTo(new UpdateOutcome(UpdateStatus.FAILED, failure, FINISHED_INSTANT));
    }

    @Test
    @DisplayName("UPDATE-POL-001: 성공 보고에 실패 상세가 있으면 저장할 곳이 없으므로 거부한다")
    void rejectsSucceededWithDetail() {
        assertThatThrownBy(() -> reportOf("SUCCEEDED", null, "detail").toOutcome())
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-POL-001: 성공 보고에 길이 제한을 넘는 실패 상세가 있어도 거부한다")
    void rejectsSucceededWithLongDetail() {
        assertThatThrownBy(() -> reportOf("SUCCEEDED", null, "a".repeat(501)).toOutcome())
                .isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    @DisplayName("UPDATE-POL-001: 비어 있는 실패 사유·상세는 보내지 않은 것으로 본다")
    void treatsBlankFailureAsAbsent(String blank) {
        assertThat(reportOf("SUCCEEDED", blank, blank).toOutcome())
                .isEqualTo(new UpdateOutcome(UpdateStatus.SUCCEEDED, null, FINISHED_INSTANT));
    }

    @Test
    @DisplayName("UPDATE-POL-001: 실패 보고에 실패 사유가 없으면 거부한다")
    void rejectsFailedWithoutReason() {
        assertThatThrownBy(() -> reportOf("FAILED", null, null).toOutcome()).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-POL-001: 성공 보고에 실패 사유가 있으면 거부한다")
    void rejectsSucceededWithReason() {
        assertThatThrownBy(() -> reportOf("SUCCEEDED", "INSTALL_FAILED", null).toOutcome())
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-POL-001: 실패 상세가 500자를 넘으면 거부한다")
    void rejectsTooLongDetail() {
        assertThatThrownBy(() ->
                        reportOf("FAILED", "INSTALL_FAILED", "a".repeat(501)).toOutcome())
                .isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"SUCCESS", "succeeded"})
    @DisplayName("UPDATE-POL-001: 정의하지 않은 업데이트 결과면 거부한다")
    void rejectsUnknownResult(String result) {
        assertThatThrownBy(() -> reportOf(result, null, null).toOutcome()).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-POL-001: 정의하지 않은 실패 사유면 거부한다")
    void rejectsUnknownFailureReason() {
        assertThatThrownBy(() -> reportOf("FAILED", "DISK_FULL", null).toOutcome())
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 캠페인 ID 가 없으면 거부한다")
    void rejectsMissingCampaignId() {
        ReportUpdateResultCommand report = new ReportUpdateResultCommand(null, "SUCCEEDED", null, null, FINISHED_AT);

        assertThatThrownBy(report::toOutcome).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 완료 시각이 없으면 거부한다")
    void rejectsMissingFinishedAt() {
        ReportUpdateResultCommand report = new ReportUpdateResultCommand(1L, "SUCCEEDED", null, null, null);

        assertThatThrownBy(report::toOutcome).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 완료 시각이 ISO-8601 형식이 아니면 원인을 담아 거부한다")
    void rejectsMalformedFinishedAt() {
        ReportUpdateResultCommand report = new ReportUpdateResultCommand(1L, "SUCCEEDED", null, null, "2026-10-01");

        assertThatThrownBy(report::toOutcome)
                .isInstanceOf(InvalidValueException.class)
                .hasCauseInstanceOf(DateTimeParseException.class);
    }

    private static ReportUpdateResultCommand reportOf(String result, String failureReason, String failureDetail) {
        return new ReportUpdateResultCommand(1L, result, failureReason, failureDetail, FINISHED_AT);
    }
}
