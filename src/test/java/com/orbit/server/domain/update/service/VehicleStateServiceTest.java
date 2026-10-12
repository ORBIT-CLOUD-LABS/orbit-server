package com.orbit.server.domain.update.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.repository.VehicleRepository;
import com.orbit.server.global.error.InvalidValueException;
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
class VehicleStateServiceTest {

    @Autowired
    private VehicleStateService vehicleStateService;

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
    @DisplayName("VEHICLE-REQ-002: 처음 등록한 차량이면 현재 버전으로 상태를 만든다")
    void createsStateForNewVehicle() {
        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.0");

        assertThat(currentVersions()).containsExactly("1.0.0");
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 재등록한 차량이면 현재 버전을 덮어쓴다")
    void overwritesVersionForReregisteredVehicle() {
        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.0");

        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.1");

        assertThat(currentVersions()).containsExactly("1.0.1");
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 현재 버전이 비어 있으면 거부한다")
    void rejectsBlankVersion() {
        assertThatThrownBy(() -> vehicleStateService.recordCurrentVersion(vehicleId, " "))
                .isInstanceOf(InvalidValueException.class);
    }

    // 엔티티에 getter 를 두지 않으므로 저장된 버전을 DB 에서 직접 읽는다
    @SuppressWarnings("unchecked")
    private List<String> currentVersions() {
        entityManager.flush();
        return entityManager
                .createNativeQuery("SELECT current_version FROM vehicle_state WHERE vehicle_id = :vehicleId")
                .setParameter("vehicleId", vehicleId)
                .getResultList();
    }
}
