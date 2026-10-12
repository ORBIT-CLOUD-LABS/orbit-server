package com.orbit.server.domain.vehicle.service;

import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import java.util.Objects;

/**
 * 차량 등록 요청.
 *
 * @param externalId 차량이 보낸 식별자
 * @param spec 차종·HW 버전·지역
 * @param currentVersion 차량의 현재 소프트웨어 버전. 형식은 차량 상태를 기록할 때 검증한다
 */
public record RegisterVehicleCommand(VehicleExternalId externalId, VehicleSpec spec, String currentVersion) {

    public RegisterVehicleCommand {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(spec, "spec");
    }

    /**
     * @return 처음 등록하는 차량
     */
    public Vehicle toVehicle() {
        return new Vehicle(externalId, spec);
    }
}
