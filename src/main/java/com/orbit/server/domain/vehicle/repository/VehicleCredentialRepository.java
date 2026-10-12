package com.orbit.server.domain.vehicle.repository;

import com.orbit.server.domain.vehicle.entity.TokenHash;
import com.orbit.server.domain.vehicle.entity.VehicleCredential;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import java.util.Optional;

/** 차량 토큰 저장소. 토큰은 해시로만 저장한다. */
public interface VehicleCredentialRepository {

    /**
     * 유효 토큰의 차량 식별자를 찾는다. 차량 API 요청마다 토큰을 확인할 때 쓴다. (VEHICLE-POL-003)
     *
     * @param tokenHash 차량이 보낸 토큰의 해시
     * @return 토큰을 발급받은 차량의 식별자. 토큰이 없거나 폐기됐으면 빈 값
     */
    Optional<VehicleExternalId> findActiveVehicleExternalId(TokenHash tokenHash);

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
