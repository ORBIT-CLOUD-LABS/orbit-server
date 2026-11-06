package com.orbit.server.domain.monitoring.repository;

import com.orbit.server.domain.monitoring.entity.CampaignResultRecord;
import com.orbit.server.domain.monitoring.service.CampaignResultCount;
import com.orbit.server.domain.monitoring.service.CampaignResultCounts;
import com.orbit.server.domain.monitoring.service.UnreportedVehicle;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 캠페인 진행 현황 저장소. 업데이트 결과(update_result)와 차량(vehicle)·현재 상태(vehicle_state) 테이블을 읽기만 한다.
 *
 * <p>대시보드 조회 모델처럼 다른 컨텍스트의 엔티티·repository 는 참조하지 않고 테이블만 읽는다. 차량 전체를 서비스로 받아 메모리에서
 * 결과와 맞춰 보지 않도록, 결과 집계와 결과를 보고하지 않은 차량 선별을 DB 에서 한다.
 *
 * <p>엔티티는 {@code @Immutable} 이라 쓰기 메서드가 없는 {@link Repository} 를 상속한다.
 */
public interface CampaignProgressRepository extends Repository<CampaignResultRecord, Long> {

    /**
     * 캠페인의 업데이트 결과를 결과·실패 사유별로 센다. 결과는 차량·캠페인당 하나라 행 수가 차량 수다.
     *
     * @param campaignId 캠페인 ID
     * @return 결과·실패 사유별 차량 수. 결과가 없으면 모두 0
     */
    default CampaignResultCounts countResults(Long campaignId) {
        return new CampaignResultCounts(findResultCounts(campaignId));
    }

    /**
     * {@link #countResults} 가 쓰는 집계. 목록 그대로 돌려주므로 직접 쓰지 않고 {@link #countResults} 를 쓴다.
     */
    // idx_update_result_campaign_result(campaign_id, result) 로 캠페인의 결과를 좁혀 센다
    @Query("""
            select new com.orbit.server.domain.monitoring.service.CampaignResultCount(
                r.result, r.failureReason, count(r))
            from CampaignResultRecord r
            where r.campaignId = :campaignId
            group by r.result, r.failureReason
            """)
    List<CampaignResultCount> findResultCounts(@Param("campaignId") Long campaignId);

    /**
     * 이 차종의 차량 중 캠페인에 업데이트 결과를 보고하지 않은 차량을 조회한다. 진행 중 판정의 후보다.
     *
     * <p>현재 상태가 없는 차량은 현재 버전을 몰라 대상인지 판정할 수 없으므로 빼고, 체크인 대상 판정과 같게 차종으로만 좁힌다. HW
     * 버전·지역·버전 범위는 캠페인 도메인이 판정한다.
     *
     * @param campaignId 캠페인 ID
     * @param model 캠페인의 대상 차종
     * @return 결과를 보고하지 않은 차량. 없으면 빈 목록
     */
    // NOT EXISTS 는 uk_update_result_vehicle_campaign(vehicle_id, campaign_id) 로 차량마다 한 번에 확인한다
    @Query(value = """
                    SELECT v.model, v.hw_version AS hwVersion, v.region, s.current_version AS currentVersion
                    FROM vehicle v
                        JOIN vehicle_state s ON s.vehicle_id = v.id
                    WHERE v.model = :model
                      AND NOT EXISTS (
                          SELECT 1
                          FROM update_result r
                          WHERE r.vehicle_id = v.id
                            AND r.campaign_id = :campaignId
                      )
                    """, nativeQuery = true)
    List<UnreportedVehicle> findUnreportedVehicles(@Param("campaignId") Long campaignId, @Param("model") String model);
}
