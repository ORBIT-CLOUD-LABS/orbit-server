package com.orbit.server.domain.monitoring.service;

import com.orbit.server.domain.monitoring.repository.VehicleUpdateHistoryRepository;
import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.error.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

/**
 * 대시보드에서 고른 차량이 어떤 버전의 업데이트를 언제 시도해 성공·실패했는지 조회한다.
 *
 * <p>이력은 차량이 결과를 보고할 때마다 쌓이고 수정하지 않으므로, 차량 확인과 이력 조회를 트랜잭션으로 묶지 않는다.
 */
@Service
@RequiredArgsConstructor
public class VehicleUpdateHistoryService {

    // 대시보드 차량 상세 패널에서 최근 흐름을 보는 용도라 페이지 없이 최근 이력만 준다
    private static final Limit RECENT_ATTEMPTS = Limit.of(50);

    private final VehicleUpdateHistoryRepository vehicleUpdateHistoryRepository;

    /**
     * 차량의 최근 업데이트 시도 50건을 완료 시각이 늦은 순으로 조회한다.
     *
     * @param vehicleId 차량이 보내는 식별자
     * @return 최근 업데이트 시도. 시도한 적이 없으면 빈 목록
     * @throws BusinessException 차량이 없으면 VEHICLE_NOT_FOUND
     */
    public List<VehicleUpdateAttempt> getRecentAttempts(String vehicleId) {
        Long vehicleKey = vehicleUpdateHistoryRepository
                .findVehicleKey(vehicleId)
                .orElseThrow(() -> new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));
        return vehicleUpdateHistoryRepository.findRecentAttempts(vehicleKey, RECENT_ATTEMPTS);
    }
}
