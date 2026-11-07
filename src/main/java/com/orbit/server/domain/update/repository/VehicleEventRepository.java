package com.orbit.server.domain.update.repository;

import com.orbit.server.domain.update.entity.UpdateFailure;
import com.orbit.server.domain.update.entity.UpdateOutcome;
import com.orbit.server.domain.update.entity.VehicleEvent;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 차량의 업데이트 시도 이력 저장소. 시도마다 한 행씩 쌓는다. */
public interface VehicleEventRepository extends JpaRepository<VehicleEvent, Long> {

    /**
     * 업데이트 시도 한 번을 이력에 남긴다. 같은 시도(차량·캠페인·완료 시각이 같은 보고)가 다시 오면 남기지 않으므로, 차량이 재전송해도 이력은
     * 하나다. 완료 시각이 다른 재시도는 따로 남는다. (UPDATE-POL-001)
     *
     * @param vehicleId 결과를 보고한 차량 ID
     * @param campaignId 결과가 속한 캠페인 ID. 존재하는 캠페인이어야 한다
     * @param outcome 성공·실패와 실패 사유, 완료 시각
     */
    default void record(Long vehicleId, Long campaignId, UpdateOutcome outcome) {
        Optional<UpdateFailure> failure = Optional.ofNullable(outcome.failure());
        insertIfAbsent(
                vehicleId,
                campaignId,
                outcome.status().name(),
                failure.map(found -> found.reason().name()).orElse(null),
                failure.map(UpdateFailure::detail).orElse(null),
                outcome.finishedAt(),
                outcome.downloadEdgeId());
    }

    /**
     * {@link #record} 가 쓰는 기록 쿼리. 값 객체를 컬럼 값으로 풀어서 받으므로 직접 쓰지 않고 {@link #record} 를 쓴다.
     */
    // 조회 후 저장하면 같은 보고가 동시에 올 때 둘 다 INSERT 해 UNIQUE(vehicle_id, campaign_id, finished_at) 에 걸린다.
    // INSERT IGNORE 는 중복 말고도 FK·검사 제약 위반까지 경고로 삼켜 버리므로, 중복일 때만 아무것도 바꾸지 않는
    // ON DUPLICATE KEY UPDATE id = id 를 쓴다. 같은 시도의 재전송이라 처음 기록을 그대로 둔다
    @Modifying(flushAutomatically = true)
    @Query(value = """
                    INSERT INTO vehicle_event
                        (vehicle_id, campaign_id, result, failure_reason, failure_detail, finished_at, edge_id)
                    VALUES
                        (:vehicleId, :campaignId, :result, :failureReason, :failureDetail, :finishedAt, :edgeId)
                    ON DUPLICATE KEY UPDATE id = id
                    """, nativeQuery = true)
    void insertIfAbsent(
            @Param("vehicleId") Long vehicleId,
            @Param("campaignId") Long campaignId,
            @Param("result") String result,
            @Param("failureReason") String failureReason,
            @Param("failureDetail") String failureDetail,
            @Param("finishedAt") Instant finishedAt,
            @Param("edgeId") String edgeId);
}
