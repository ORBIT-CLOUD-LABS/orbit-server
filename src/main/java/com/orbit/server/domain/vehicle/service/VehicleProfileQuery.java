package com.orbit.server.domain.vehicle.service;

import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import java.util.Optional;

/**
 * 등록한 차량 조회.
 *
 * <p>DB 에서 조회 결과 형태로 바로 읽는 조회 전용 경로라 쓰기용 {@link com.orbit.server.domain.vehicle.repository.VehicleRepository}
 * 와 나눈다. 구현은 repository 에 둔다.
 */
public interface VehicleProfileQuery {

    /**
     * 잠그지 않고 조회한다. 체크인은 차량을 읽기만 하므로 재등록과 서로 기다리지 않게 한다.
     *
     * @param externalId 차량이 보낸 식별자
     * @return 이 식별자의 차량 정보. 등록하지 않은 차량이면 빈 값
     */
    Optional<VehicleProfile> findByExternalId(VehicleExternalId externalId);
}
