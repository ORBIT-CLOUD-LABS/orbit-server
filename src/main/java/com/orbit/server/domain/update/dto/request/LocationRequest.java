package com.orbit.server.domain.update.dto.request;

import com.orbit.server.domain.update.entity.VehicleLocation;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.validation.constraints.NotNull;

/**
 * 체크인에 담아 보내는 차량의 현재 GPS 위치. (VEHICLE-REQ-001)
 *
 * <p>좌표가 빠지면 0 으로 바뀌어 적도 위의 엉뚱한 위치가 되므로 래퍼 타입으로 받아 빠졌는지 확인한다. 범위는 {@link VehicleLocation} 이
 * 검증해 규칙을 한 곳에 둔다.
 *
 * @param latitude 위도. -90 ~ 90
 * @param longitude 경도. -180 ~ 180
 */
public record LocationRequest(
        @NotNull Double latitude, @NotNull Double longitude) {

    /**
     * @return 차량 위치
     * @throws InvalidValueException 위도나 경도가 범위를 벗어난 경우
     */
    public VehicleLocation toLocation() {
        return new VehicleLocation(latitude, longitude);
    }
}
