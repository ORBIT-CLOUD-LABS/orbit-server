package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.VehicleStatusSnapshot;
import java.time.Instant;
import java.util.List;

/**
 * 대시보드 차량 상태 스냅샷 응답. (MONITORING-REQ-004)
 *
 * @param asOf 조회 기준 시각(DB 시각)
 * @param vehicles 전체 차량 상태. vehicleId 오름차순
 */
public record VehicleStatusSnapshotResponse(Instant asOf, List<VehicleStatusResponse> vehicles) {

    /**
     * @param snapshot 조회한 스냅샷
     * @return 같은 값을 담은 응답
     */
    public static VehicleStatusSnapshotResponse from(VehicleStatusSnapshot snapshot) {
        return new VehicleStatusSnapshotResponse(
                snapshot.asOf(),
                snapshot.vehicles().stream().map(VehicleStatusResponse::from).toList());
    }
}
