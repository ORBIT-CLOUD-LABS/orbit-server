package com.orbit.server.domain.monitoring.service;

import com.orbit.server.domain.monitoring.repository.VehicleStatusViewRepository;
import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 대시보드에 보여 줄 차량 상태와 차량 상세를 조회한다. (MONITORING-REQ-004)
 *
 * <p>조회 한 번에 기준 시각과 차량 목록을 읽는 순서만 지키면 되고, 상세는 한 번의 조회라 트랜잭션으로 묶지 않는다.
 */
@Service
@RequiredArgsConstructor
public class VehicleStatusQueryService {

    private final VehicleStatusViewRepository vehicleStatusViewRepository;

    /**
     * 대시보드가 SSE 를 연결한 뒤 처음 화면을 채울 전체 차량 상태를 조회한다. (MONITORING-POL-001, MONITORING-POL-003)
     *
     * @return DB 시각 기준의 스냅샷. 차량은 vehicleId 오름차순, 없으면 빈 목록
     */
    public VehicleStatusSnapshot getSnapshot() {
        return vehicleStatusViewRepository.findSnapshot();
    }

    /**
     * 대시보드에서 고른 차량 한 대의 상세와 최근 업데이트 결과를 조회한다.
     *
     * @param vehicleId 차량이 보내는 식별자
     * @return 차량 상세
     * @throws BusinessException 차량이 없으면 VEHICLE_NOT_FOUND
     */
    public VehicleDetail getDetail(String vehicleId) {
        return vehicleStatusViewRepository
                .findDetail(vehicleId)
                .orElseThrow(() -> new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));
    }
}
