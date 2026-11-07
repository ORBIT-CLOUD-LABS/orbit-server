package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.VehicleLastLocation;
import java.time.Instant;

/**
 * [Phase2] 차량 상세의 마지막 위치.
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
