package com.orbit.server.domain.update.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbit.server.domain.update.service.CheckInResult;

/**
 * 체크인 응답. (VEHICLE-REQ-001)
 *
 * @param updateTarget 업데이트할 캠페인이 있으면 true
 * @param campaignId 업데이트할 캠페인 ID. 대상이 아니면 응답에서 뺀다
 * @param nextCheckInSeconds 차량이 다음 체크인까지 기다릴 시간(초)
 */
public record CheckInResponse(
        boolean updateTarget,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long campaignId,
        int nextCheckInSeconds) {

    public static CheckInResponse from(CheckInResult result) {
        return new CheckInResponse(
                result.updateTarget(), result.targetCampaignId().orElse(null), result.nextCheckInSeconds());
    }
}
