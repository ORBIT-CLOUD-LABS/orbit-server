package com.orbit.server.domain.vehicle.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.vehicle.entity.TokenHash;
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleCredential;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.entity.VehicleToken;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class VehicleCredentialJpaRepositoryTest {

    @Autowired
    private VehicleCredentialRepository credentialRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private EntityManager entityManager;

    private Long vehicleId;

    @BeforeEach
    void setUp() {
        Vehicle vehicle = new Vehicle(new VehicleExternalId("vehicle-001"), new VehicleSpec("model-a", "hw-1", "KR"));
        vehicleId = vehicleRepository.save(vehicle).id();
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 토큰을 저장하면 DB 가 기록한 발급 시각이 채워진다")
    void fillsIssuedAtOnSave() {
        VehicleCredential saved = credentialRepository.save(newCredential());

        assertThat(saved.issuedAt()).isNotNull();
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 유효 토큰을 폐기 상태와 폐기 시각으로 바꾼다")
    void revokesActiveCredential() {
        credentialRepository.save(newCredential());

        int revoked = credentialRepository.revokeActiveByVehicleId(vehicleId);

        assertThat(revoked).isEqualTo(1);
        assertThat(statusesWithRevokedAt()).containsExactly("REVOKED:true");
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 기존 토큰을 폐기하면 새 토큰을 유효 상태로 저장할 수 있다")
    void savesNewCredentialAfterRevoking() {
        credentialRepository.save(newCredential());

        credentialRepository.revokeActiveByVehicleId(vehicleId);
        credentialRepository.save(newCredential());
        entityManager.flush();

        assertThat(statusesWithRevokedAt()).containsExactlyInAnyOrder("REVOKED:true", "ACTIVE:false");
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 처음 등록하는 차량이면 폐기할 토큰이 없다")
    void revokesNothingForNewVehicle() {
        assertThat(credentialRepository.revokeActiveByVehicleId(vehicleId)).isZero();
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 유효 토큰 해시로 차량 식별자를 찾는다")
    void findsVehicleExternalIdByActiveToken() {
        TokenHash tokenHash = VehicleToken.issue().hash();
        credentialRepository.save(new VehicleCredential(vehicleId, tokenHash));
        entityManager.flush();

        assertThat(credentialRepository.findActiveVehicleExternalId(tokenHash))
                .contains(new VehicleExternalId("vehicle-001"));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 폐기한 토큰 해시로는 차량을 찾지 않는다")
    void findsNothingByRevokedToken() {
        TokenHash tokenHash = VehicleToken.issue().hash();
        credentialRepository.save(new VehicleCredential(vehicleId, tokenHash));
        credentialRepository.revokeActiveByVehicleId(vehicleId);

        assertThat(credentialRepository.findActiveVehicleExternalId(tokenHash)).isEmpty();
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 발급하지 않은 토큰 해시로는 차량을 찾지 않는다")
    void findsNothingByUnknownToken() {
        credentialRepository.save(newCredential());
        entityManager.flush();

        assertThat(credentialRepository.findActiveVehicleExternalId(
                        VehicleToken.issue().hash()))
                .isEmpty();
    }

    private VehicleCredential newCredential() {
        return new VehicleCredential(vehicleId, VehicleToken.issue().hash());
    }

    // 엔티티에 getter 를 두지 않으므로 상태와 폐기 시각 기록 여부를 DB 에서 직접 읽는다
    @SuppressWarnings("unchecked")
    private List<String> statusesWithRevokedAt() {
        return entityManager
                .createNativeQuery("SELECT CONCAT(status, ':', IF(revoked_at IS NULL, 'false', 'true'))"
                        + " FROM vehicle_credential WHERE vehicle_id = :vehicleId")
                .setParameter("vehicleId", vehicleId)
                .getResultList();
    }
}
