package com.orbit.server.domain.vehicle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.EntityManager;
import java.util.List;
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
class VehicleRegistrationServiceTest {

    private static final String VEHICLE_ID = "vehicle-001";
    private static final VehicleSpec SPEC = new VehicleSpec("model-a", "hw-1", "KR");

    @Autowired
    private VehicleRegistrationService service;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("VEHICLE-REQ-002: 처음 등록하면 차량·상태를 만들고 유효 토큰을 발급한다")
    void registersNewVehicle() {
        VehicleRegistrationResult result = service.register(commandOf(SPEC, "1.0.0"));

        assertThat(result.created()).isTrue();
        assertThat(result.vehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(result.issuedAt()).isNotNull();
        assertThat(queryStrings("SELECT CONCAT(v.model, '/', s.current_version) FROM vehicle v"
                        + " JOIN vehicle_state s ON s.vehicle_id = v.id WHERE v.external_id = :vehicleId"))
                .containsExactly("model-a/1.0.0");
        assertThat(activeTokenHashes())
                .containsExactly(result.vehicleToken().hash().value());
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 재등록하면 차량 정보와 현재 버전을 갱신한다")
    void updatesReregisteredVehicle() {
        service.register(commandOf(SPEC, "1.0.0"));

        VehicleRegistrationResult result =
                service.register(commandOf(new VehicleSpec("model-a", "hw-2", "US"), "1.0.1"));

        assertThat(result.created()).isFalse();
        assertThat(queryStrings("SELECT CONCAT(v.hw_version, '/', v.region, '/', s.current_version) FROM vehicle v"
                        + " JOIN vehicle_state s ON s.vehicle_id = v.id WHERE v.external_id = :vehicleId"))
                .containsExactly("hw-2/US/1.0.1");
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 재등록하면 기존 토큰을 폐기하고 새 토큰 하나만 유효하다")
    void revokesPreviousTokenOnReregistration() {
        VehicleRegistrationResult first = service.register(commandOf(SPEC, "1.0.0"));

        VehicleRegistrationResult second = service.register(commandOf(SPEC, "1.0.0"));

        assertThat(second.vehicleToken()).isNotEqualTo(first.vehicleToken());
        assertThat(activeTokenHashes())
                .containsExactly(second.vehicleToken().hash().value());
        assertThat(queryStrings("SELECT c.token_hash FROM vehicle_credential c JOIN vehicle v ON v.id = c.vehicle_id"
                        + " WHERE v.external_id = :vehicleId AND c.status = 'REVOKED' AND c.revoked_at IS NOT NULL"))
                .containsExactly(first.vehicleToken().hash().value());
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 현재 버전이 비어 있으면 거부한다")
    void rejectsBlankCurrentVersion() {
        assertThatThrownBy(() -> service.register(commandOf(SPEC, " "))).isInstanceOf(InvalidValueException.class);
    }

    private RegisterVehicleCommand commandOf(VehicleSpec spec, String currentVersion) {
        return new RegisterVehicleCommand(new VehicleExternalId(VEHICLE_ID), spec, currentVersion);
    }

    private List<String> activeTokenHashes() {
        return queryStrings("SELECT c.token_hash FROM vehicle_credential c JOIN vehicle v ON v.id = c.vehicle_id"
                + " WHERE v.external_id = :vehicleId AND c.status = 'ACTIVE'");
    }

    // 엔티티에 getter 를 두지 않으므로 저장 결과를 DB 에서 직접 읽는다
    @SuppressWarnings("unchecked")
    private List<String> queryStrings(String sql) {
        entityManager.flush();
        return entityManager
                .createNativeQuery(sql)
                .setParameter("vehicleId", VEHICLE_ID)
                .getResultList();
    }
}
