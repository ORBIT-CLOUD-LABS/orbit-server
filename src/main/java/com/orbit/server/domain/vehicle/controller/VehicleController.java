package com.orbit.server.domain.vehicle.controller;

import com.orbit.server.domain.vehicle.dto.request.RegisterVehicleRequest;
import com.orbit.server.domain.vehicle.dto.response.VehicleRegistrationResponse;
import com.orbit.server.domain.vehicle.service.VehicleRegistrationResult;
import com.orbit.server.domain.vehicle.service.VehicleRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 차량이 호출하는 API. */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleRegistrationService vehicleRegistrationService;

    /**
     * 차량을 등록하고 차량별 토큰을 발급한다. 재등록하면 기존 토큰은 폐기된다. (VEHICLE-REQ-002 / VEHICLE-POL-003)
     *
     * <p>공통 등록 키 확인은 이 경로에 건 인터셉터가 먼저 한다.
     *
     * @param request 차량 식별자, 차종, HW 버전, 지역, 현재 버전
     * @return 처음 등록이면 201, 재등록이면 200 과 새 토큰
     */
    @PostMapping("/register")
    public ResponseEntity<VehicleRegistrationResponse> register(@Valid @RequestBody RegisterVehicleRequest request) {
        VehicleRegistrationResult result = vehicleRegistrationService.register(request.toCommand());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(VehicleRegistrationResponse.from(result));
    }
}
