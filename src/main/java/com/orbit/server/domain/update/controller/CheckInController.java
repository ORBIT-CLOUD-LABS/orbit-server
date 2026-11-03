package com.orbit.server.domain.update.controller;

import com.orbit.server.domain.update.dto.request.CheckInRequest;
import com.orbit.server.domain.update.dto.response.CheckInResponse;
import com.orbit.server.domain.update.service.CheckInService;
import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.config.ApiTags;
import com.orbit.server.global.error.ApiErrorCodes;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 차량이 주기적으로 호출하는 체크인 API. */
@Tag(name = ApiTags.VEHICLE_UPDATE)
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * 차량의 현재 버전을 기록하고 업데이트할 캠페인이 있는지 응답한다. (VEHICLE-REQ-001 / VEHICLE-POL-001)
     *
     * <p>차량 토큰과 경로 {@code vehicleId} 확인은 이 경로에 건 인터셉터가 먼저 한다.
     *
     * @param vehicleId 차량 식별자
     * @param request 현재 버전
     * @return 대상 여부, 대상 캠페인 ID, 다음 체크인까지의 시간
     */
    @PostMapping("/{vehicleId}/check-in")
    @ApiErrorCodes(
            type = VehicleErrorCode.class,
            codes = {"VEHICLE_MISMATCH"})
    public CheckInResponse checkIn(@PathVariable String vehicleId, @Valid @RequestBody CheckInRequest request) {
        return CheckInResponse.from(checkInService.checkIn(request.toCommand(vehicleId)));
    }
}
