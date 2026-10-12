package com.orbit.server.domain.vehicle.service;

import com.orbit.server.domain.vehicle.entity.VehicleToken;
import java.time.Instant;

/**
 * 차량 등록 결과.
 *
 * @param created 처음 등록이면 true, 재등록이면 false. 응답 상태(201/200)를 가른다
 * @param vehicleId 차량이 보낸 식별자
 * @param vehicleToken 새로 발급한 토큰 원문. 저장하지 않으므로 이 결과에서만 꺼낼 수 있다
 * @param issuedAt 토큰 발급 시각
 */
public record VehicleRegistrationResult(
        boolean created, String vehicleId, VehicleToken vehicleToken, Instant issuedAt) {}
