package com.orbit.server.domain.update.repository;

import com.orbit.server.domain.update.entity.UpdateFailure;
import com.orbit.server.domain.update.entity.UpdateOutcome;
import com.orbit.server.domain.update.entity.UpdateResult;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 차량이 보고한 업데이트 결과 저장소. 차량·캠페인당 결과를 하나만 둔다. */
public interface UpdateResultRepository extends JpaRepository<UpdateResult, Long> {

    /**
     * 업데이트 결과를 기록한다. 같은 차량·캠페인의 결과가 이미 있으면 덮어쓰므로 같은 보고가 다시 와도 결과는 같다. (UPDATE-POL-001)
     *
     * @param vehicleId 결과를 보고한 차량 ID
     * @param campaignId 결과가 속한 캠페인 ID. 존재하는 캠페인이어야 한다
     * @param outcome 성공·실패와 실패 사유, 완료 시각
     */
    default void record(Long vehicleId, Long campaignId, UpdateOutcome outcome) {
        Optional<UpdateFailure> failure = Optional.ofNullable(outcome.failure());
        upsert(
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
    // 조회 후 저장하면 같은 보고가 동시에 올 때 둘 다 INSERT 해 UNIQUE(vehicle_id, campaign_id) 에 걸리므로
    // INSERT ... ON DUPLICATE KEY UPDATE 로 한 번에 덮어쓴다. VALUES() 함수는 MySQL 8.0.20 부터 deprecated 라 행 별칭을 쓴다
    // 중복이 있으면 기존 행을 고친다.
    @Modifying(flushAutomatically = true)
    @Query(value = """
                    INSERT INTO update_result
                        (vehicle_id, campaign_id, result, failure_reason, failure_detail, finished_at, edge_id)
                    VALUES
                        (:vehicleId, :campaignId, :result, :failureReason, :failureDetail, :finishedAt, :edgeId)
                        AS reported
                    ON DUPLICATE KEY UPDATE
                        result = reported.result,
                        failure_reason = reported.failure_reason,
                        failure_detail = reported.failure_detail,
                        finished_at = reported.finished_at,
                        edge_id = reported.edge_id
                    """, nativeQuery = true)
    void upsert(
            @Param("vehicleId") Long vehicleId,
            @Param("campaignId") Long campaignId,
            @Param("result") String result,
            @Param("failureReason") String failureReason,
            @Param("failureDetail") String failureDetail,
            @Param("finishedAt") Instant finishedAt,
            @Param("edgeId") String edgeId);
}
