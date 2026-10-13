package com.orbit.server.domain.monitoring.service;

import java.time.Instant;

/**
 * 대시보드용 차량 상태 조회. 구현은 repository 에 둔다.
 *
 * <p>대시보드는 쓰기 없이 읽기만 하는 조회 모델이라, 차량(vehicle)·현재 상태(vehicle_state)·업데이트 결과(update_result) 테이블을
 * 한 번에 조인해 읽는다. 다른 컨텍스트의 엔티티·repository 는 참조하지 않고 테이블만 읽는다. 서비스를 거쳐 따로 받아 합치면 SSE 가
 * 1초마다 차량 전체를 메모리에서 다시 조립해야 하기 때문이다.
 */
public interface VehicleStatusQuery {

    /**
     * 전체 차량의 현재 상태를 조회한다. 차량별 최근 업데이트 결과는 보고 시각(reported_at)이 늦은 것이고, 같으면 나중에 기록한 것이다.
     * (MONITORING-REQ-004 / MONITORING-POL-001, MONITORING-POL-003)
     *
     * @return DB 시각 기준의 스냅샷. 차량은 vehicleId 오름차순
     */
    VehicleStatusSnapshot findSnapshot();

    /**
     * 상태 변경 시각(vehicle_state.updated_at)이 주어진 시각보다 늦은 차량을 조회한다. (MONITORING-REQ-001 / MONITORING-POL-001)
     *
     * <p>updated_at 은 커밋이 아니라 UPDATE 실행 시각이므로, 늦게 커밋된 변경을 놓치지 않으려면 호출하는 쪽이 이전 기준 시각보다 앞선
     * 시각을 넘겨 겹쳐 조회해야 한다.
     *
     * @param since 이 시각보다 늦게 바뀐 차량만 조회한다. 같은 시각은 포함하지 않는다
     * @return DB 시각 기준의 변경분. 차량은 vehicleId 오름차순, 없으면 빈 목록
     */
    VehicleStatusChanges findChangesSince(Instant since);
}
