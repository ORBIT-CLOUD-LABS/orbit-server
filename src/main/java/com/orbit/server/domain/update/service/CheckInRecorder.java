package com.orbit.server.domain.update.service;

import com.orbit.server.global.error.InvalidValueException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 체크인으로 받은 차량 상태와 업데이트 결과를 기록한다. (VEHICLE-REQ-001, UPDATE-REQ-001)
 *
 * <p>상태와 결과는 항상 함께, 체크인 트랜잭션 안에서 기록하므로 {@link CheckInService} 에서 떼어 한 협력 객체로 둔다.
 */
@Component
@RequiredArgsConstructor
class CheckInRecorder {

    private final VehicleStateService vehicleStateService;
    private final UpdateResultService updateResultService;

    /**
     * 현재 버전·마지막 체크인 시각을 기록하고, 보낸 위치와 보고한 업데이트 결과가 있으면 함께 기록한다. 위치가 없으면(GPS 미수신) 이전
     * 위치를 그대로 둔다.
     *
     * @param vehicleId 체크인한 차량 ID
     * @param command 체크인 요청
     * @throws InvalidValueException 현재 버전이 비어 있거나 50자를 넘거나 점으로 나눈 숫자 형식이 아닌 경우
     */
    // 결과가 기록되면 vehicle_state.updated_at 도 함께 바뀌어야 SSE 변경분에 잡히므로, 체크인 트랜잭션 밖에서는 부르지 못하게 한다
    @Transactional(propagation = Propagation.MANDATORY)
    void record(Long vehicleId, CheckInCommand command) {
        vehicleStateService.recordCheckIn(vehicleId, command.currentVersion());
        command.location().ifPresent(location -> vehicleStateService.recordLocation(vehicleId, location));
        command.lastUpdate().ifPresent(report -> updateResultService.record(vehicleId, report));
    }
}
