package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.CampaignStats;
import com.orbit.server.domain.monitoring.service.UpdateFailureReason;
import java.util.Map;

/**
 * 캠페인 진행 현황 응답. {@code inProgress = targetVehicles - succeeded - failed} 이다.
 *
 * @param campaignId 캠페인 ID
 * @param targetVehicles 대상 차량 수. 성공·실패·진행 중 차량의 합
 * @param succeeded 업데이트에 성공한 차량 수
 * @param failed 업데이트에 실패한 차량 수
 * @param failureReasons 실패 사유별 차량 수. 보고된 적 없는 사유도 0 으로 담는다
 * @param inProgress 결과를 보고하지 않았고 대상 조건에 맞으며 아직 목표 버전이 아닌 차량 수
 */
public record CampaignStatsResponse(
        Long campaignId,
        long targetVehicles,
        long succeeded,
        long failed,
        Map<UpdateFailureReason, Long> failureReasons,
        long inProgress) {

    /**
     * @param stats 조회한 진행 현황
     * @return 같은 값을 담은 응답
     */
    public static CampaignStatsResponse from(CampaignStats stats) {
        return new CampaignStatsResponse(
                stats.campaignId(),
                stats.targetVehicles(),
                stats.succeeded(),
                stats.failed(),
                stats.failureReasons(),
                stats.inProgress());
    }
}
