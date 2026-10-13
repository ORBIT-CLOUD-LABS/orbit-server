package com.orbit.server.domain.monitoring.repository;

import com.orbit.server.domain.monitoring.entity.VehicleStatusView;
import com.orbit.server.domain.monitoring.service.VehicleStatus;
import com.orbit.server.domain.monitoring.service.VehicleStatusQuery;
import com.orbit.server.domain.monitoring.service.VehicleStatusSnapshot;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Spring Data JPA 로 구현한 {@link VehicleStatusQuery}.
 *
 * <p>VehicleStatusView 에 getter 를 두지 않으므로 엔티티를 읽지 않고 JPQL 생성자 표현식으로 바로 {@link VehicleStatus} 를 만든다.
 */
interface VehicleStatusJpaQuery extends Repository<VehicleStatusView, String>, VehicleStatusQuery {

    // 기준 시각을 차량보다 먼저 읽어, 스냅샷에 담긴 커밋된 변경이 기준 시각보다 늦지 않게 한다.
    // 기준 시각은 변경분 조회의 안전한 커서가 아니다. updated_at 은 커밋이 아니라 UPDATE 실행 시각이라,
    // 조회 때 커밋 전이던 체크인은 기준 시각보다 이른 updated_at 으로 늦게 커밋될 수 있다.
    // 누락 방지는 SSE 변경분 조회가 커서보다 앞선 구간을 겹쳐 다시 읽어 맡는다 (#17)
    @Override
    default VehicleStatusSnapshot findSnapshot() {
        Instant asOf = currentTime().toInstant(ZoneOffset.UTC);
        return new VehicleStatusSnapshot(asOf, findAllStatuses());
    }

    // 여러 pod 가 같은 시계를 따르도록 DB 시각을 쓴다. SSE 변경분 조회와 같은 밀리초 정밀도로 맞춘다.
    // 네이티브 조회는 DATETIME 을 시간대 없는 LocalDateTime 으로 주므로, 세션 시간대(UTC 고정) 기준으로 받아 Instant 로 바꾼다
    @Query(value = "SELECT NOW(3)", nativeQuery = true)
    LocalDateTime currentTime();

    @Query("""
            select new com.orbit.server.domain.monitoring.service.VehicleStatus(
                s.vehicleId, s.model, s.currentVersion, s.lastSeenAt, s.updatedAt,
                s.campaignId, s.lastResult, s.failureReason)
            from VehicleStatusView s
            order by s.vehicleId
            """)
    List<VehicleStatus> findAllStatuses();
}
