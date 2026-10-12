package com.orbit.server.domain.vehicle.repository;

import com.orbit.server.domain.vehicle.entity.VehicleCredential;

/** 차량 토큰 저장소. 토큰은 해시로만 저장한다. */
public interface VehicleCredentialRepository {

    /**
     * 차량의 유효 토큰을 폐기한다. 새 토큰을 발급하기 전에 호출해야 차량당 유효 토큰 하나 제약을 어기지 않는다. (VEHICLE-POL-003)
     *
     * @param vehicleId 차량 ID
     * @return 폐기한 토큰 수. 처음 등록하는 차량이면 0
     */
    int revokeActiveByVehicleId(Long vehicleId);

    /**
     * 토큰을 저장한다.
     *
     * @param credential 저장할 토큰
     * @return ID 와 발급 시각이 채워진 토큰
     */
    VehicleCredential save(VehicleCredential credential);
}
