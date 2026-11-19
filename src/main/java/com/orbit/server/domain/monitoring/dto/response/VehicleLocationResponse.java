package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.result.VehicleLastLocation;
import java.time.Instant;

/**
 * [Phase2] 차량의 마지막 위치. 차량 상태(스냅샷)와 차량 상세가 같은 형식을 쓴다.
 *
 * @param latitude 위도
 * @param longitude 경도
 * @param updatedAt 위치를 받은 시각
 */
public record VehicleLocationResponse(double latitude, double longitude, Instant updatedAt) {

    /**
     * @param location 조회한 마지막 위치
     * @return 같은 값을 담은 응답
     */
    public static VehicleLocationResponse from(VehicleLastLocation location) {
        return new VehicleLocationResponse(location.latitude(), location.longitude(), location.updatedAt());
    }
}
