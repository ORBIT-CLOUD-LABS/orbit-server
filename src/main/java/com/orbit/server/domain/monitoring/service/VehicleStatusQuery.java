package com.orbit.server.domain.monitoring.service;

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
}
