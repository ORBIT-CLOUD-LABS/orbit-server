package com.orbit.server.domain.vehicle.service;

import com.orbit.server.domain.update.service.VehicleStateService;
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleCredential;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.entity.VehicleToken;
import com.orbit.server.domain.vehicle.repository.VehicleCredentialRepository;
import com.orbit.server.domain.vehicle.repository.VehicleRepository;
import com.orbit.server.global.error.InvalidValueException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차량을 등록하고 차량별 토큰을 발급한다. 재등록하면 기존 토큰을 폐기해 차량당 유효 토큰을 하나로 유지한다. (VEHICLE-REQ-002 /
 * VEHICLE-POL-003)
 *
 * <p>다른 컨텍스트인 차량 상태는 {@link VehicleStateService} 로만 기록한다.
 */
@Service
@RequiredArgsConstructor
public class VehicleRegistrationService {

    private final VehicleRepository vehicleRepository;
    private final VehicleCredentialRepository vehicleCredentialRepository;
    private final VehicleStateService vehicleStateService;

    /**
     * 차량 잠금·생성 또는 갱신 → 현재 버전 기록 → 기존 토큰 폐기 → 새 토큰 발급 순서로 등록한다.
     *
     * <p>같은 차량의 재등록이 동시에 와도 토큰 폐기·발급이 엇갈리지 않도록 차량 행을 커밋할 때까지 잠근다. 잠글 행이 없는 최초 등록이
     * 동시에 오면 한쪽은 식별자 UNIQUE 제약에 걸려 실패하고, 차량은 다시 등록한다.
     *
     * @param command 등록 요청
     * @return 등록 결과와 새 토큰 원문
     * @throws InvalidValueException 현재 버전이 비어 있거나 50자를 넘는 경우
     * @throws DataIntegrityViolationException 같은 차량의 최초 등록이 동시에 들어온 경우
     */
    // 차량 잠금을 토큰 발급·커밋까지 유지하고, 중간에 실패하면 차량·상태·토큰을 함께 되돌리기 위해 한 트랜잭션으로 묶는다.
    // MySQL 기본(REPEATABLE READ)에서는 없는 차량을 잠글 때 식별자 인덱스의 gap 을 잠가, 이웃한 식별자의 최초 등록끼리
    // 서로의 INSERT 를 막아 데드락이 난다. READ COMMITTED 는 gap 을 잠그지 않아 서로 다른 차량은 막지 않고,
    // 같은 차량끼리는 행 잠금과 UNIQUE 제약으로 여전히 직렬화된다
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VehicleRegistrationResult register(RegisterVehicleCommand command) {
        Optional<Vehicle> registered = vehicleRepository.lockByExternalId(command.externalId());
        Vehicle vehicle = registered
                .map(found -> reregister(found, command.spec()))
                .orElseGet(() -> vehicleRepository.save(command.toVehicle()));
        vehicleStateService.recordCurrentVersion(vehicle.id(), command.currentVersion());
        VehicleToken token = VehicleToken.issue();
        VehicleCredential credential = reissue(vehicle.id(), token);
        return new VehicleRegistrationResult(
                registered.isEmpty(), command.externalId().value(), token, credential.issuedAt());
    }

    private Vehicle reregister(Vehicle vehicle, VehicleSpec spec) {
        vehicle.reregister(spec);
        return vehicle;
    }

    // 차량당 유효 토큰 하나 제약(uk_vehicle_credential_active_vehicle)을 어기지 않도록 폐기를 먼저 한다
    private VehicleCredential reissue(Long vehicleId, VehicleToken token) {
        vehicleCredentialRepository.revokeActiveByVehicleId(vehicleId);
        return vehicleCredentialRepository.save(new VehicleCredential(vehicleId, token.hash()));
    }
}
