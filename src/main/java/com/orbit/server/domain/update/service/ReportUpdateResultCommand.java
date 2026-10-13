package com.orbit.server.domain.update.service;

import com.orbit.server.domain.update.entity.FailureReason;
import com.orbit.server.domain.update.entity.UpdateFailure;
import com.orbit.server.domain.update.entity.UpdateOutcome;
import com.orbit.server.domain.update.entity.UpdateStatus;
import com.orbit.server.global.error.InvalidValueException;
import java.time.Instant;
import java.util.Objects;

/**
 * 차량이 체크인에 담아 보낸 업데이트 결과({@code lastUpdate}). (UPDATE-REQ-001)
 *
 * @param campaignId 결과가 속한 캠페인 ID. null 불가
 * @param result 성공 또는 실패
 * @param failureReason 실패 사유. 실패일 때만 있다
 * @param failureDetail 실패 상세. 선택이며 실패 사유가 없으면 쓰지 않는다
 * @param finishedAt 차량이 업데이트를 마친 시각
 */
public record ReportUpdateResultCommand(
        Long campaignId, UpdateStatus result, FailureReason failureReason, String failureDetail, Instant finishedAt) {

    public ReportUpdateResultCommand {
        Objects.requireNonNull(campaignId, "campaignId");
    }

    /**
     * 보고를 업데이트 결과로 바꾼다. 실패 여부와 실패 사유 유무가 맞는지는 {@link UpdateOutcome} 이 검증한다. (UPDATE-POL-001)
     *
     * @return 성공·실패와 실패 정보, 완료 시각
     * @throws InvalidValueException 결과나 완료 시각이 없거나, 실패 여부와 실패 사유 유무가 맞지 않거나, 실패 상세가 500자를 넘는 경우
     */
    public UpdateOutcome toOutcome() {
        return new UpdateOutcome(result, toFailure(), finishedAt);
    }

    // 실패 사유가 없으면 실패 정보가 없는 것으로 보고, 실패 여부와 맞는지는 UpdateOutcome 에 맡긴다
    private UpdateFailure toFailure() {
        if (failureReason == null) {
            return null;
        }
        return new UpdateFailure(failureReason, failureDetail);
    }
}
