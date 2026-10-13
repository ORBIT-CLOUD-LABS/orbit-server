package com.orbit.server.domain.monitoring.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 한 시점의 전체 차량 상태. 대시보드가 SSE 를 연결한 뒤 처음 화면을 채울 때 쓴다. (MONITORING-REQ-004)
 *
 * @param asOf 조회 기준 시각. 여러 pod 가 같은 시계를 따르도록 DB 시각을 쓴다. null 불가
 * @param vehicles 전체 차량 상태. null 불가, 차량이 없으면 빈 목록
 */
public record VehicleStatusSnapshot(Instant asOf, List<VehicleStatus> vehicles) {

    /**
     * @throws NullPointerException 기준 시각이나 차량 목록이 없는 경우
     */
    public VehicleStatusSnapshot {
        Objects.requireNonNull(asOf, "asOf");
        vehicles = List.copyOf(vehicles);
    }
}
