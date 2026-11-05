package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.update.entity.FailureReason;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateFailureReasonTest {

    // 차량이 보고하는 사유가 늘었는데 진행 현황에만 빠지는 일을 막는다
    @Test
    @DisplayName("캠페인 진행 현황: 실패 사유 목록이 차량이 보고하는 실패 사유와 같다")
    void matchesReportedFailureReasons() {
        assertThat(Arrays.stream(UpdateFailureReason.values()).map(Enum::name))
                .containsExactlyElementsOf(
                        Arrays.stream(FailureReason.values()).map(Enum::name).toList());
    }
}
