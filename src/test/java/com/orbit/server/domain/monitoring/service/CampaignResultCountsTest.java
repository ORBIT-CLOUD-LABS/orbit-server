package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CampaignResultCountsTest {

    @Test
    @DisplayName("캠페인 진행 현황: 결과가 없으면 성공·실패와 모든 사유별 수가 0 이다")
    void countsZeroWithoutResults() {
        CampaignResultCounts counts = new CampaignResultCounts(List.of());

        assertThat(counts.succeeded()).isZero();
        assertThat(counts.failed()).isZero();
        assertThat(counts.failureReasons())
                .hasSize(UpdateFailureReason.values().length)
                .allSatisfy((reason, count) -> assertThat(count).isZero());
    }

    @Test
    @DisplayName("캠페인 진행 현황: 실패는 사유별로 나누고 합계도 낸다")
    void countsFailuresByReason() {
        CampaignResultCounts counts = new CampaignResultCounts(List.of(
                new CampaignResultCount("SUCCEEDED", null, 80),
                new CampaignResultCount("FAILED", "DOWNLOAD_FAILED", 2),
                new CampaignResultCount("FAILED", "INSTALL_FAILED", 3)));

        assertThat(counts.succeeded()).isEqualTo(80);
        assertThat(counts.failed()).isEqualTo(5);
        assertThat(counts.failureReasons())
                .containsExactly(
                        entry(UpdateFailureReason.DOWNLOAD_FAILED, 2L),
                        entry(UpdateFailureReason.HASH_MISMATCH, 0L),
                        entry(UpdateFailureReason.SIGNATURE_INVALID, 0L),
                        entry(UpdateFailureReason.DOWNGRADE_REJECTED, 0L),
                        entry(UpdateFailureReason.INSTALL_FAILED, 3L));
    }

    @Test
    @DisplayName("캠페인 진행 현황: 모르는 실패 사유는 사유별 수에서 빠지지만 실패 합계에는 들어간다")
    void countsUnknownReasonOnlyInTotal() {
        CampaignResultCounts counts =
                new CampaignResultCounts(List.of(new CampaignResultCount("FAILED", "UNKNOWN", 1)));

        assertThat(counts.failed()).isEqualTo(1);
        assertThat(counts.failureReasons().values()).containsOnly(0L);
    }
}
