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

    /**
     * 체크인한 차량의 현재 버전과 마지막 체크인 시각을 덮어쓴다. (VEHICLE-REQ-001)
     *
     * @param vehicleId 차량 ID
     * @param currentVersion 차량이 보낸 현재 버전
     * @throws InvalidValueException 버전이 비어 있거나 50자를 넘는 경우
     * @throws IllegalStateException 차량의 상태가 없는 경우
     */
    // 등록할 때 상태를 함께 만들므로 체크인한 차량에 상태가 없다면 데이터가 어긋난 것이라 서버 오류로 본다
    @Transactional
    public void recordCheckIn(Long vehicleId, String currentVersion) {
        int updated = vehicleStateRepository.recordCheckIn(vehicleId, new SoftwareVersion(currentVersion));
        if (updated == 0) {
            throw new IllegalStateException("체크인한 차량의 상태가 없습니다: " + vehicleId);
        }
    }

    /**
     * 마지막 체크인 때 기록한 차량의 현재 버전을 조회한다. 매니페스트 발급에서 대상을 다시 판정할 때 쓴다. (MANIFEST-POL-001)
     *
     * @param vehicleId 차량 ID
     * @return 차량의 현재 버전
     * @throws IllegalStateException 차량의 상태가 없는 경우
     */
    // 등록할 때 상태를 함께 만들므로 인증을 통과한 차량에 상태가 없다면 데이터가 어긋난 것이라 서버 오류로 본다
    public SoftwareVersion getCurrentVersion(Long vehicleId) {
        return vehicleStateRepository
                .findCurrentVersionById(vehicleId)
                .orElseThrow(() -> new IllegalStateException("차량의 상태가 없습니다: " + vehicleId));
    }
}
