package com.orbit.server.domain.monitoring.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 어떤 시각 이후 상태가 바뀐 차량들. 대시보드 SSE 가 주기마다 보내는 변경분이다. (MONITORING-REQ-001)
 *
 * @param asOf 조회 기준 시각. 차량을 읽기 전의 DB 시각이며 SSE 이벤트 id 로 쓴다. null 불가
 * @param vehicles 상태가 바뀐 차량. null 불가, 바뀐 차량이 없으면 빈 목록
 */
public record VehicleStatusChanges(Instant asOf, List<VehicleStatus> vehicles) {

    /**
     * @throws NullPointerException 기준 시각이나 차량 목록이 없는 경우
     */
    public VehicleStatusChanges {
        Objects.requireNonNull(asOf, "asOf");
        vehicles = List.copyOf(vehicles);
    }
}
