package com.orbit.server.domain.monitoring.repository;

import com.orbit.server.domain.monitoring.domain.CampaignResultRecord;
import com.orbit.server.domain.monitoring.service.EdgeDownloadCount;
import com.orbit.server.domain.monitoring.service.EdgeVehicleCount;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * 대시보드 CDN Edge 상태가 쓰는 Edge 별 사용 현황 저장소. 차량 현재 상태(vehicle_state)와 업데이트 결과(update_result) 테이블을
 * 읽기만 한다.
 *
 * <p>대시보드 조회 모델처럼 다른 컨텍스트의 엔티티·repository 는 참조하지 않고 테이블만 읽는다. 차량 전체를 서비스로 받아 메모리에서 세지
 * 않도록 DB 에서 Edge 별로 묶어 센다.
 *
 * <p>Edge 는 DB 가 아니라 배포 설정에만 있어 여기서는 Edge id 로만 묶는다. 설정에서 빠진 Edge 의 기록도 함께 나오므로, 설정의 Edge 만
 * 고르는 것은 호출하는 쪽이 한다.
 *
 * <p>엔티티는 {@code @Immutable} 이라 쓰기 메서드가 없는 {@link Repository} 를 상속한다.
 */
public interface EdgeUsageRepository extends Repository<CampaignResultRecord, Long> {

    /**
     * Edge 별로 배정된 차량 수를 센다. 배정된 Edge 가 없는 차량은 세지 않는다.
     *
     * @return 배정된 차량이 있는 Edge 와 그 차량 수. 없으면 빈 목록
     */
    // vehicle_state 는 monitoring 에 매핑한 엔티티가 없어 네이티브 쿼리로 읽는다.
    // assigned_edge_id 에는 인덱스가 없어 vehicle_state 전체를 읽는다. SSE 처럼 주기적으로 돌지 않고 대시보드가 호출할 때만
    // 실행하므로 인덱스를 두지 않았다 (#88)
    @Query(value = """
                    SELECT s.assigned_edge_id AS edgeId, COUNT(*) AS vehicles
                    FROM vehicle_state s
                    WHERE s.assigned_edge_id IS NOT NULL
                    GROUP BY s.assigned_edge_id
                    """, nativeQuery = true)
    List<EdgeVehicleCount> findAssignedVehicleCounts();

    /**
     * 차량이 실제로 파일을 받은 Edge 와 캠페인별로 업데이트 성공·실패 수를 센다. 결과는 차량·캠페인당 하나라 행 수가 차량 수다.
     *
     * <p>받은 Edge 가 없는 결과(다운로드 전 실패, Phase1 기록)는 어느 Edge 에도 세지 않는다. 캠페인 상태는 보지 않으므로 비활성 캠페인의
     * 결과도 센다.
     *
     * @return 결과가 있는 Edge·캠페인과 그 성공·실패 수. Edge 안에서는 campaignId 오름차순, 없으면 빈 목록
     */
    // edge_id 에는 인덱스가 없어 update_result 전체를 읽는다. 배정 차량 수와 같은 이유로 인덱스를 두지 않았다 (#88)
    @Query("""
            select new com.orbit.server.domain.monitoring.service.EdgeDownloadCount(
                r.edgeId, r.campaignId,
                sum(case when r.result = 'SUCCEEDED' then 1L else 0L end),
                sum(case when r.result = 'FAILED' then 1L else 0L end))
            from CampaignResultRecord r
            where r.edgeId is not null
            group by r.edgeId, r.campaignId
            order by r.edgeId, r.campaignId
            """)
    List<EdgeDownloadCount> findDownloadCounts();
}
