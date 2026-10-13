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

/**
 * Spring Data JPA 로 구현한 {@link UpdateResultRepository}.
 *
 * <p>service 는 {@link UpdateResultRepository} 로만 주입받으므로, {@link JpaRepository} 의 삭제·일괄 저장 같은
 * 메서드는 밖에서 호출할 수 없다.
 */
interface UpdateResultJpaRepository extends JpaRepository<UpdateResult, Long>, UpdateResultRepository {

    @Override
    default void record(Long vehicleId, Long campaignId, UpdateOutcome outcome) {
        Optional<UpdateFailure> failure = Optional.ofNullable(outcome.failure());
        upsert(
                vehicleId,
                campaignId,
                outcome.status().name(),
                failure.map(found -> found.reason().name()).orElse(null),
                failure.map(UpdateFailure::detail).orElse(null),
                outcome.finishedAt());
    }

    // 조회 후 저장하면 같은 보고가 동시에 올 때 둘 다 INSERT 해 UNIQUE(vehicle_id, campaign_id) 에 걸리므로
    // INSERT ... ON DUPLICATE KEY UPDATE 로 한 번에 덮어쓴다. VALUES() 함수는 MySQL 8.0.20 부터 deprecated 라 행 별칭을 쓴다
    // 중복이 있으면 기존 행을 고친다.
    @Modifying(flushAutomatically = true)
    @Query(value = """
                    INSERT INTO update_result (vehicle_id, campaign_id, result, failure_reason, failure_detail, finished_at)
                    VALUES (:vehicleId, :campaignId, :result, :failureReason, :failureDetail, :finishedAt) AS reported
                    ON DUPLICATE KEY UPDATE
                        result = reported.result,
                        failure_reason = reported.failure_reason,
                        failure_detail = reported.failure_detail,
                        finished_at = reported.finished_at
                    """, nativeQuery = true)
    void upsert(
            @Param("vehicleId") Long vehicleId,
            @Param("campaignId") Long campaignId,
            @Param("result") String result,
            @Param("failureReason") String failureReason,
            @Param("failureDetail") String failureDetail,
            @Param("finishedAt") Instant finishedAt);
}
