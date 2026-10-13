package com.orbit.server.domain.monitoring.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 대시보드에 보여 줄 차량 상태를 조회한다. (MONITORING-REQ-004)
 *
 * <p>조회 한 번에 기준 시각과 차량 목록을 읽는 순서만 지키면 되므로 트랜잭션으로 묶지 않는다.
 */
@Service
@RequiredArgsConstructor
public class VehicleStatusQueryService {

    private final VehicleStatusQuery vehicleStatusQuery;

    /**
     * 대시보드가 SSE 를 연결한 뒤 처음 화면을 채울 전체 차량 상태를 조회한다. (MONITORING-POL-001, MONITORING-POL-003)
     *
     * @return DB 시각 기준의 스냅샷. 차량은 vehicleId 오름차순, 없으면 빈 목록
     */
    public VehicleStatusSnapshot getSnapshot() {
        return vehicleStatusQuery.findSnapshot();
    }
}
