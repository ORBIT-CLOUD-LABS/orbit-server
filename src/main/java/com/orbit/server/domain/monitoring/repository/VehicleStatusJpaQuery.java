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

    // 기준 시각을 차량보다 먼저 읽는다. 그 사이 바뀐 차량은 스냅샷에 이미 반영돼 있으므로,
    // 기준 시각 이후 변경분을 다시 받아도 중복될 뿐 빠지지 않는다
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
