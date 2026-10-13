package com.orbit.server.domain.update.dto.request;

import com.orbit.server.domain.update.entity.FailureReason;
import com.orbit.server.domain.update.entity.UpdateStatus;
import com.orbit.server.domain.update.service.ReportUpdateResultCommand;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;

/**
 * 체크인에 담아 보내는 업데이트 결과. 차량은 체크인이 성공할 때까지 같은 결과를 다시 보낸다. (UPDATE-REQ-001)
 *
 * <p>실패 여부와 실패 사유 유무가 맞는지는 도메인에서 검증한다.
 *
 * @param campaignId 결과가 속한 캠페인 ID
 * @param result {@code SUCCEEDED} 또는 {@code FAILED}
 * @param failureReason 실패 사유. 실패일 때만 보낸다
 * @param failureDetail 실패 상세. 선택, 500자 이하
 * @param finishedAt 업데이트를 마친 시각 (ISO-8601)
 */
public record LastUpdateRequest(
        @NotNull Long campaignId,
        @NotNull UpdateStatus result,
        FailureReason failureReason,
        String failureDetail,
        @NotNull Instant finishedAt) {

    /**
     * @return 업데이트 결과 기록 요청
     */
    public ReportUpdateResultCommand toCommand() {
        return new ReportUpdateResultCommand(
                campaignId,
                result.name(),
                Optional.ofNullable(failureReason).map(FailureReason::name).orElse(null),
                failureDetail,
                finishedAt.toString());
    }
}
