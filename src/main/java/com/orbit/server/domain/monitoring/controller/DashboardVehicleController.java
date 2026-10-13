package com.orbit.server.domain.monitoring.controller;

import com.orbit.server.domain.monitoring.dto.response.VehicleStatusSnapshotResponse;
import com.orbit.server.domain.monitoring.service.VehicleStatusQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 대시보드용 차량 상태 API. 관리자 토큰 인증은 SecurityConfig 가 경로 단위로 적용한다. */
@RestController
@RequestMapping("/api/v1/dashboard/vehicles")
@RequiredArgsConstructor
public class DashboardVehicleController {

    private final VehicleStatusQueryService vehicleStatusQueryService;

    /**
     * 전체 차량 상태 스냅샷을 조회한다. 대시보드는 SSE 를 먼저 연결한 뒤 이 API 를 부른다.
     * (MONITORING-REQ-004 / MONITORING-POL-001, MONITORING-POL-003)
     *
     * @return 200 과 기준 시각, vehicleId 오름차순의 전체 차량 상태
     */
    @GetMapping
    public VehicleStatusSnapshotResponse getSnapshot() {
        return VehicleStatusSnapshotResponse.from(vehicleStatusQueryService.getSnapshot());
    }
}
