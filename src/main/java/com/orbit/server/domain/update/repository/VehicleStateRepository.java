package com.orbit.server.domain.update.repository;

import com.orbit.server.domain.update.entity.VehicleState;
import java.util.Optional;

/** 차량 현재 상태 저장소. */
public interface VehicleStateRepository {

    /**
     * 차량의 현재 상태를 조회한다.
     *
     * @param vehicleId 차량 ID
     * @return 차량의 상태. 아직 기록한 적이 없으면 빈 값
     */
    Optional<VehicleState> findById(Long vehicleId);

    /**
     * 차량의 상태를 저장한다.
     *
     * @param state 저장할 상태
     * @return 저장한 상태
     */
    VehicleState save(VehicleState state);
}
