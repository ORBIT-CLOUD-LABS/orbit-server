package com.orbit.server.domain.update.service;

import com.orbit.server.domain.update.entity.SoftwareVersion;
import com.orbit.server.domain.update.entity.VehicleState;
import com.orbit.server.domain.update.repository.VehicleStateRepository;
import com.orbit.server.global.error.InvalidValueException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차량의 현재 상태를 기록한다. 다른 컨텍스트(차량 등록)는 VehicleState 를 직접 쓰지 않고 이 서비스로만 기록한다.
 * (VEHICLE-REQ-002)
 */
@Service
@RequiredArgsConstructor
public class VehicleStateService {

    private final VehicleStateRepository vehicleStateRepository;

    /**
     * 차량의 현재 버전을 기록한다. 처음이면 상태를 만들고, 있으면 덮어쓴다.
     *
     * <p>같은 차량의 요청이 동시에 오면 상태를 두 번 만들 수 있으므로, 호출한 쪽이 차량 행을 잠근 트랜잭션 안에서 불러야 한다.
     *
     * @param vehicleId 차량 ID
     * @param currentVersion 차량이 보낸 현재 버전
     * @throws InvalidValueException 버전이 비어 있거나 50자를 넘는 경우
     */
    // 조회한 상태를 변경 감지로 덮어쓰므로 조회부터 저장까지 한 트랜잭션으로 묶는다
    @Transactional
    public void recordCurrentVersion(Long vehicleId, String currentVersion) {
        SoftwareVersion version = new SoftwareVersion(currentVersion);
        vehicleStateRepository
                .findById(vehicleId)
                .ifPresentOrElse(
                        state -> state.changeVersion(version),
                        () -> vehicleStateRepository.save(new VehicleState(vehicleId, version)));
    }
}
