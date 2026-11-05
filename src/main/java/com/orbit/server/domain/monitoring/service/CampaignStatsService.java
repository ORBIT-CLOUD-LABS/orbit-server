package com.orbit.server.domain.monitoring.service;

import com.orbit.server.domain.campaign.service.CampaignDetailService;
import com.orbit.server.domain.campaign.service.CampaignQueryService;
import com.orbit.server.domain.campaign.service.TargetVehicleCriteria;
import com.orbit.server.domain.monitoring.repository.CampaignProgressRepository;
import com.orbit.server.global.error.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캠페인 진행 현황을 조회한다.
 *
 * <p>결과 집계와 결과를 보고하지 않은 차량은 테이블에서 바로 읽고, 그 차량이 대상인지는 다른 컨텍스트인 캠페인에
 * {@link CampaignQueryService} 로 묻는다. 버전 범위를 숫자 자리로 비교하는 규칙을 캠페인 도메인 한 곳에만 두기 위해서다.
 */
@Service
@RequiredArgsConstructor
public class CampaignStatsService {

    private final CampaignDetailService campaignDetailService;
    private final CampaignQueryService campaignQueryService;
    private final CampaignProgressRepository campaignProgressRepository;

    /**
     * 캠페인의 성공·실패(사유별)·진행 중 차량 수를 조회한다. 비활성이거나 적용 기간이 끝난 캠페인도 조회한다.
     *
     * @param campaignId 캠페인 ID
     * @return 진행 현황
     * @throws BusinessException 캠페인이 없으면 CAMPAIGN_NOT_FOUND
     */
    // 결과 집계와 미보고 차량 조회 사이에 결과가 들어와 한 차량이 두 번 세이지 않도록 한 트랜잭션의 같은 스냅샷에서 읽는다
    @Transactional(readOnly = true)
    public CampaignStats getStats(Long campaignId) {
        String model = campaignDetailService.getById(campaignId).target().model();
        CampaignResultCounts results = campaignProgressRepository.countResults(campaignId);
        List<TargetVehicleCriteria> unreported =
                campaignProgressRepository.findUnreportedVehicles(campaignId, model).stream()
                        .map(CampaignStatsService::toCriteria)
                        .toList();
        long inProgress = campaignQueryService.countPendingTargets(campaignId, unreported);
        return CampaignStats.of(campaignId, results, inProgress);
    }

    private static TargetVehicleCriteria toCriteria(UnreportedVehicle vehicle) {
        return new TargetVehicleCriteria(
                vehicle.model(), vehicle.hwVersion(), vehicle.region(), vehicle.currentVersion());
    }
}
