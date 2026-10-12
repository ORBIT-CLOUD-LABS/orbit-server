package com.orbit.server.domain.vehicle.repository;

import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import java.util.Optional;

/** 등록한 차량 저장소. */
public interface VehicleRepository {

    /**
     * 차량을 조회하며 트랜잭션이 끝날 때까지 잠근다. 같은 차량의 재등록이 동시에 들어와도 토큰 폐기·발급을 순서대로 처리하기 위해 쓴다.
     * (VEHICLE-POL-003)
     *
     * <p>트랜잭션 안에서 호출해야 한다. 트랜잭션 밖에서는 조회가 끝나는 즉시 잠금이 풀린다.
     *
     * @param externalId 차량이 보낸 식별자
     * @return 잠근 차량. 처음 등록하는 차량이면 빈 값
     */
    Optional<Vehicle> lockByExternalId(VehicleExternalId externalId);

    /**
     * 차량을 저장한다.
     *
     * @param vehicle 저장할 차량
     * @return ID 가 채워진 차량
     */
    Vehicle save(Vehicle vehicle);
}
