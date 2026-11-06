package com.orbit.server.domain.monitoring.service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 캠페인 진행 현황. 대상 차량을 성공·실패·진행 중으로 나눈다.
 *
 * <p>성공한 차량은 현재 버전이 목표 버전으로 바뀌어 대상 조건의 버전 범위를 벗어날 수 있다. 그래서 대상 차량을 지금 조건에 맞는 차량으로
 * 세지 않고, 결과를 보고한 차량과 진행 중 차량을 더해 센다.
 *
 * @param campaignId 캠페인 ID. null 불가
 * @param succeeded 업데이트에 성공한 차량 수
 * @param failed 업데이트에 실패한 차량 수
 * @param failureReasons 실패 사유별 차량 수. 모든 사유를 담는다. null 불가
 * @param inProgress 결과를 보고하지 않았고 대상 조건에 맞으며 아직 목표 버전이 아닌 차량 수
 */
public record CampaignStats(
        Long campaignId, long succeeded, long failed, Map<UpdateFailureReason, Long> failureReasons, long inProgress) {

    public CampaignStats {
        Objects.requireNonNull(campaignId, "campaignId");
        // 응답의 사유 순서가 요청마다 바뀌지 않도록 사유 선언 순서를 지키는 EnumMap 으로 복사한다
        failureReasons =
                Collections.unmodifiableMap(new EnumMap<>(Objects.requireNonNull(failureReasons, "failureReasons")));
    }

    /**
     * @param campaignId 캠페인 ID
     * @param results 결과·실패 사유별 차량 수
     * @param inProgress 진행 중 차량 수
     * @return 진행 현황
     */
    public static CampaignStats of(Long campaignId, CampaignResultCounts results, long inProgress) {
        return new CampaignStats(
                campaignId, results.succeeded(), results.failed(), results.failureReasons(), inProgress);
    }

    /**
     * @return 대상 차량 수. 성공·실패·진행 중 차량을 더한다
     */
    public long targetVehicles() {
        return succeeded + failed + inProgress;
    }
}
