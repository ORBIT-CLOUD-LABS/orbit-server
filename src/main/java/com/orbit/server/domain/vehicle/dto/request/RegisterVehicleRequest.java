package com.orbit.server.domain.vehicle.dto.request;

import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.service.RegisterVehicleCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 차량 등록 요청. 차종·HW 버전·지역은 등록 때만 받으므로, 바뀌면 차량이 다시 등록한다.
 *
 * @param vehicleId 차량 식별자
 * @param model 차종
 * @param hwVersion HW 버전
 * @param region 지역
 * @param currentVersion 현재 소프트웨어 버전
 */
public record RegisterVehicleRequest(
        @NotBlank String vehicleId,
        @NotBlank String model,
        @NotBlank String hwVersion,
        @NotBlank String region,
        @NotBlank String currentVersion) {

    public RegisterVehicleCommand toCommand() {
        return new RegisterVehicleCommand(
                new VehicleExternalId(vehicleId), new VehicleSpec(model, hwVersion, region), currentVersion);
    }
}
