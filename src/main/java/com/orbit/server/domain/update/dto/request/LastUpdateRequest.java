package com.orbit.server.domain.update.dto.request;

import com.orbit.server.domain.update.service.ReportUpdateResultCommand;

/**
 * 체크인에 담아 보내는 업데이트 결과. 차량은 체크인이 성공할 때까지 같은 결과를 다시 보낸다. (UPDATE-REQ-001)
 *
 * <p>형식이 틀린 결과 때문에 체크인 요청 전체가 400 으로 거부되면 그 차량의 체크인이 계속 실패한다. 그래서 enum·시각도 문자열로 받고
 * Bean Validation 을 걸지 않는다. 검증은 {@link ReportUpdateResultCommand#toOutcome()} 에서 하고, 틀린 결과는 버린다.
 *
 * @param campaignId 결과가 속한 캠페인 ID
 * @param result {@code SUCCEEDED} 또는 {@code FAILED}
 * @param failureReason 실패 사유. 실패일 때만 보낸다
 * @param failureDetail 실패 상세. 실패 사유가 있을 때만 보낼 수 있다. 500자 이하
 * @param finishedAt 업데이트를 마친 시각 (ISO-8601)
 */
public record LastUpdateRequest(
        Long campaignId, String result, String failureReason, String failureDetail, String finishedAt) {

    /**
     * @return 업데이트 결과 기록 요청
     */
    public ReportUpdateResultCommand toCommand() {
        return new ReportUpdateResultCommand(campaignId, result, failureReason, failureDetail, finishedAt);
    }
}
