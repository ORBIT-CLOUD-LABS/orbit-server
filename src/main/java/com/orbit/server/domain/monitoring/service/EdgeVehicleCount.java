package com.orbit.server.domain.monitoring.service;

import java.util.Objects;

/**
 * 한 Edge 에 배정된 차량 수. DB 집계 한 행이다.
 *
 * @param edgeId 매니페스트가 차량에 마지막으로 배정한 Edge 의 id. null 불가
 * @param vehicles 이 Edge 에 배정된 차량 수
 */
public record EdgeVehicleCount(String edgeId, long vehicles) {

    public EdgeVehicleCount {
        Objects.requireNonNull(edgeId, "edgeId");
    }
}
