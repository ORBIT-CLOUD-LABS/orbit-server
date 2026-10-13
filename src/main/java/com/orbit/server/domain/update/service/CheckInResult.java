package com.orbit.server.domain.update.service;

import java.util.Objects;
import java.util.Optional;

/**
 * 체크인 결과. 업데이트할 캠페인이 있으면 그 ID 를, 없으면 대상 아님을 알린다. (VEHICLE-REQ-001)
 *
 * <p>대상이 아닐 때 캠페인 ID 를 null 로 두지 않도록 {@link Optional} 로 감싼다.
 *
 * @param targetCampaignId 업데이트할 캠페인 ID. 대상이 아니면 빈 값
 * @param nextCheckInSeconds 차량이 다음 체크인까지 기다릴 시간(초)
 */
public record CheckInResult(Optional<Long> targetCampaignId, int nextCheckInSeconds) {

    public CheckInResult {
        Objects.requireNonNull(targetCampaignId, "targetCampaignId");
    }

    /**
     * @return 업데이트할 캠페인이 있으면 true
     */
    public boolean updateTarget() {
        return targetCampaignId.isPresent();
    }
}
