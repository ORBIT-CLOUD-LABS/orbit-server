package com.orbit.server.domain.update.service;

import com.orbit.server.domain.update.entity.SoftwareVersion;
import com.orbit.server.domain.update.entity.VehicleLocation;
import com.orbit.server.domain.update.entity.VehicleState;
import com.orbit.server.domain.update.repository.ManifestStateRow;
import com.orbit.server.domain.update.repository.VehicleStateRepository;
import com.orbit.server.domain.update.service.ManifestVehicleState.LastLocation;
import com.orbit.server.global.error.InvalidValueException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차량의 현재 상태를 기록한다. 다른 컨텍스트(차량 등록)는 VehicleState 를 직접 쓰지 않고 이 서비스로만 기록한다.
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
     * @throws InvalidValueException 버전이 비어 있거나 50자를 넘거나 점으로 나눈 숫자 형식이 아닌 경우
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
     * 체크인한 차량의 현재 버전과 마지막 체크인 시각을 덮어쓴다.
     *
     * @param vehicleId 차량 ID
     * @param currentVersion 차량이 보낸 현재 버전
     * @throws InvalidValueException 버전이 비어 있거나 50자를 넘거나 점으로 나눈 숫자 형식이 아닌 경우
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
     * 체크인한 차량의 위치와 위치 갱신 시각을 덮어쓴다. 위치는 Edge 선택에만 쓰고 대상 판정에는 쓰지 않는다.
     *
     * <p>체크인 기록({@link #recordCheckIn})과 같은 트랜잭션에서 부르므로 상태가 있는지는 다시 확인하지 않는다.
     *
     * @param vehicleId 차량 ID
     * @param location 차량이 보낸 위치
     */
    @Transactional
    public void recordLocation(Long vehicleId, VehicleLocation location) {
        vehicleStateRepository.recordLocation(vehicleId, location);
    }

    /**
     * 매니페스트 발급에 쓸 차량 상태(현재 버전, 마지막 위치와 받은 뒤 지난 시간)를 한 번에 조회한다.
     *
     * @param vehicleId 차량 ID
     * @return 차량 상태
     * @throws IllegalStateException 차량의 상태가 없는 경우
     */
    // 등록할 때 상태를 함께 만들므로 인증을 통과한 차량에 상태가 없다면 데이터가 어긋난 것이라 서버 오류로 본다
    public ManifestVehicleState getManifestState(Long vehicleId) {
        ManifestStateRow row = vehicleStateRepository
                .findManifestStateById(vehicleId)
                .orElseThrow(() -> new IllegalStateException("차량의 상태가 없습니다: " + vehicleId));
        return new ManifestVehicleState(vehicleId, new SoftwareVersion(row.getCurrentVersion()), lastLocationOf(row));
    }

    /**
     * 매니페스트가 차량에 고른 Edge 를 기록한다.
     *
     * @param vehicleId 차량 ID
     * @param edgeId 고른 Edge 의 id. Edge 설정에서 검증된 값이다
     * @throws IllegalStateException 차량의 상태가 없는 경우
     */
    @Transactional
    public void assignEdge(Long vehicleId, String edgeId) {
        int updated = vehicleStateRepository.recordAssignedEdge(vehicleId, edgeId);
        if (updated == 0) {
            throw new IllegalStateException("Edge 를 배정할 차량의 상태가 없습니다: " + vehicleId);
        }
    }

    // 위도·경도·갱신 시각은 DB 검사 제약으로 함께 있거나 함께 없으므로 위도만 보고 위치 유무를 판단한다
    private static Optional<LastLocation> lastLocationOf(ManifestStateRow row) {
        if (row.getLatitude() == null) {
            return Optional.empty();
        }
        VehicleLocation location = new VehicleLocation(
                row.getLatitude().doubleValue(), row.getLongitude().doubleValue());
        return Optional.of(new LastLocation(location, Duration.of(row.getLocationAgeMicros(), ChronoUnit.MICROS)));
    }
}
