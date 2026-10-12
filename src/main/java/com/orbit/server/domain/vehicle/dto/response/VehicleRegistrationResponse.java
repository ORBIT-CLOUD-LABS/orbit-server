package com.orbit.server.domain.vehicle.dto.response;

import com.orbit.server.domain.vehicle.service.VehicleRegistrationResult;
import java.time.Instant;

/**
 * 차량 등록 응답.
 *
 * @param vehicleId 차량 식별자
 * @param vehicleToken 차량 토큰 원문. 서버는 해시만 저장하므로 이 응답에서 한 번만 내려준다
 * @param issuedAt 토큰 발급 시각
 */
public record VehicleRegistrationResponse(String vehicleId, String vehicleToken, Instant issuedAt) {

    public static VehicleRegistrationResponse from(VehicleRegistrationResult result) {
        return new VehicleRegistrationResponse(
                result.vehicleId(), result.vehicleToken().value(), result.issuedAt());
    }
}
