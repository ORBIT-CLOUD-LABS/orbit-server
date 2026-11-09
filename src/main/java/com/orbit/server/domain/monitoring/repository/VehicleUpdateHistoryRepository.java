package com.orbit.server.domain.monitoring.repository;

import com.orbit.server.domain.monitoring.entity.VehicleEventRecord;
import com.orbit.server.domain.monitoring.service.VehicleUpdateAttempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 차량 업데이트 이력 저장소. 업데이트 시도 이력(vehicle_event)과 차량(vehicle)·캠페인(campaign)·artifact 테이블을 읽기만 한다.
 *
 * <p>대시보드 조회 모델처럼 다른 컨텍스트의 엔티티·repository 는 참조하지 않고 테이블만 읽는다.
 *
 * <p>엔티티는 {@code @Immutable} 이라 쓰기 메서드가 없는 {@link Repository} 를 상속한다.
 */
public interface VehicleUpdateHistoryRepository extends Repository<VehicleEventRecord, Long> {

    /**
     * 차량이 보내는 식별자로 이력을 찾을 차량 ID 를 조회한다.
     *
     * <p>이력이 없는 차량과 없는 차량을 구분하려고 이력 조회와 따로 둔다.
     *
     * @param vehicleId 차량이 보내는 식별자
     * @return 차량 ID. 차량이 없으면 빈 값
     */
    // uk_vehicle_external_id 로 한 행을 찾는다
    @Query(value = "SELECT id FROM vehicle WHERE external_id = :vehicleId", nativeQuery = true)
    Optional<Long> findVehicleKey(@Param("vehicleId") String vehicleId);

    /**
     * 차량의 업데이트 시도 이력을 완료 시각이 늦은 순으로 조회한다. 완료 시각이 같으면 나중에 기록한 것이 먼저다.
     *
     * @param vehicleKey 차량 ID ({@link #findVehicleKey} 의 결과)
     * @param limit 최대 개수
     * @return 최근 업데이트 시도. 없으면 빈 목록
     */
    // idx_vehicle_event_vehicle_finished(vehicle_id, finished_at) 를 역순으로 읽어 정렬 없이 limit 만큼만 읽는다
    @Query("""
            select new com.orbit.server.domain.monitoring.service.VehicleUpdateAttempt(
                e.campaignId, e.version, e.result, e.failureReason, e.failureDetail, e.edgeId, e.finishedAt)
            from VehicleEventRecord e
            where e.vehicleId = :vehicleKey
            order by e.finishedAt desc, e.id desc
            """)
    List<VehicleUpdateAttempt> findRecentAttempts(@Param("vehicleKey") Long vehicleKey, Limit limit);
}
