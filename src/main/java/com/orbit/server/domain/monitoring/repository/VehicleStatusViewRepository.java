package com.orbit.server.domain.monitoring.repository;

import com.orbit.server.domain.monitoring.entity.VehicleStatusView;
import com.orbit.server.domain.monitoring.service.VehicleStatus;
import com.orbit.server.domain.monitoring.service.VehicleStatusChanges;
import com.orbit.server.domain.monitoring.service.VehicleStatusSnapshot;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * 대시보드용 차량 상태 저장소. vehicle_status 뷰를 읽기만 한다.
 *
 * <p>대시보드는 쓰기 없이 읽기만 하는 조회 모델이라, 차량(vehicle)·현재 상태(vehicle_state)·업데이트 결과(update_result) 테이블을
 * 한 번에 조인한 뷰를 읽는다. 다른 컨텍스트의 엔티티·repository 는 참조하지 않고 테이블만 읽는다. 서비스를 거쳐 따로 받아 합치면 SSE 가
 * 1초마다 차량 전체를 메모리에서 다시 조립해야 하기 때문이다.
 *
 * <p>뷰는 {@code @Immutable} 이라 쓰기 메서드가 없는 {@link Repository} 를 상속한다. 차량 전체를 1초마다 읽으므로 엔티티 대신 JPQL
 * 생성자 표현식으로 바로 {@link VehicleStatus} 를 만든다.
 */
public interface VehicleStatusViewRepository extends Repository<VehicleStatusView, String> {

    /**
     * 전체 차량의 현재 상태를 조회한다. 차량별 최근 업데이트 결과는 보고 시각(reported_at)이 늦은 것이고, 같으면 나중에 기록한 것이다.
     * (MONITORING-REQ-004 / MONITORING-POL-001, MONITORING-POL-003)
     *
     * @return DB 시각 기준의 스냅샷. 차량은 vehicleId 오름차순
     */
    // 기준 시각을 차량보다 먼저 읽어, 스냅샷에 담긴 커밋된 변경이 기준 시각보다 늦지 않게 한다.
    // 기준 시각은 변경분 조회의 안전한 커서가 아니다. updated_at 은 커밋이 아니라 UPDATE 실행 시각이라,
    // 조회 때 커밋 전이던 체크인은 기준 시각보다 이른 updated_at 으로 늦게 커밋될 수 있다.
    // 누락 방지는 SSE 변경분 조회가 커서보다 앞선 구간을 겹쳐 다시 읽어 맡는다 (#17)
    default VehicleStatusSnapshot findSnapshot() {
        Instant asOf = Instant.ofEpochMilli(currentEpochMilli());
        return new VehicleStatusSnapshot(asOf, findAllStatuses());
    }

    /**
     * 상태 변경 시각(vehicle_state.updated_at)이 주어진 시각보다 늦은 차량을 조회한다. (MONITORING-REQ-001 / MONITORING-POL-001)
     *
     * <p>updated_at 은 커밋이 아니라 UPDATE 실행 시각이므로, 늦게 커밋된 변경을 놓치지 않으려면 호출하는 쪽이 이전 기준 시각보다 앞선
     * 시각을 넘겨 겹쳐 조회해야 한다.
     *
     * @param since 이 시각보다 늦게 바뀐 차량만 조회한다. 같은 시각은 포함하지 않는다
     * @return DB 시각 기준의 변경분. 차량은 vehicleId 오름차순, 없으면 빈 목록
     */
    // 스냅샷과 같은 이유로 기준 시각을 차량보다 먼저 읽는다
    default VehicleStatusChanges findChangesSince(Instant since) {
        Instant asOf = Instant.ofEpochMilli(currentEpochMilli());
        return new VehicleStatusChanges(asOf, findStatusesUpdatedAfter(since));
    }

    /**
     * {@link #findSnapshot} 과 {@link #findChangesSince} 의 기준 시각. 직접 쓰지 않고 두 메서드를 쓴다.
     */
    // 여러 pod 가 같은 시계를 따르도록 DB 시각을 쓰고, updated_at 과 같은 밀리초 정밀도로 맞춘다.
    // 네이티브 조회의 DATETIME 은 Hibernate 가 JVM 시간대의 LocalDateTime 으로 바꿔 주고, 반환 타입을 Instant 로 두면
    // Spring Data 가 변환하지 못한다. 시간대 변환을 거치지 않도록 epoch 밀리초로 받는다.
    // NOW 와 UNIX_TIMESTAMP 는 같은 세션 시간대로 해석하므로 세션 시간대와 상관없이 같은 순간이 된다
    @Query(value = "SELECT CAST(UNIX_TIMESTAMP(NOW(3)) * 1000 AS SIGNED)", nativeQuery = true)
    long currentEpochMilli();

    /**
     * {@link #findSnapshot} 이 쓰는 조회. 기준 시각 없이 읽으므로 직접 쓰지 않고 {@link #findSnapshot} 을 쓴다.
     */
    @Query("""
            select new com.orbit.server.domain.monitoring.service.VehicleStatus(
                s.vehicleId, s.model, s.currentVersion, s.lastSeenAt, s.updatedAt,
                s.campaignId, s.lastResult, s.failureReason)
            from VehicleStatusView s
            order by s.vehicleId
            """)
    List<VehicleStatus> findAllStatuses();

    /**
     * {@link #findChangesSince} 가 쓰는 조회. 기준 시각 없이 읽으므로 직접 쓰지 않고 {@link #findChangesSince} 를 쓴다.
     */
    // 뷰가 MERGE 로 처리돼 updated_at 조건이 vehicle_state 로 들어가므로 idx_vehicle_state_updated_at 을 탄다
    @Query("""
            select new com.orbit.server.domain.monitoring.service.VehicleStatus(
                s.vehicleId, s.model, s.currentVersion, s.lastSeenAt, s.updatedAt,
                s.campaignId, s.lastResult, s.failureReason)
            from VehicleStatusView s
            where s.updatedAt > :since
            order by s.vehicleId
            """)
    List<VehicleStatus> findStatusesUpdatedAfter(Instant since);
}
