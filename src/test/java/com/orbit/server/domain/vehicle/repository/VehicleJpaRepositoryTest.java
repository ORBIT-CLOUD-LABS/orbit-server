package com.orbit.server.domain.vehicle.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import jakarta.persistence.EntityManager;
import java.util.Optional;
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
class VehicleJpaRepositoryTest {

    private static final VehicleExternalId EXTERNAL_ID = new VehicleExternalId("vehicle-001");
    private static final VehicleSpec SPEC = new VehicleSpec("model-a", "hw-1", "KR");

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("VEHICLE-POL-003: 등록한 차량을 식별자로 잠그며 조회한다")
    void locksSavedVehicle() {
        Vehicle saved = vehicleRepository.save(new Vehicle(EXTERNAL_ID, SPEC));

        Optional<Vehicle> locked = vehicleRepository.lockByExternalId(EXTERNAL_ID);

        assertThat(locked).map(Vehicle::id).contains(saved.id());
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 처음 등록하는 차량이면 빈 값을 돌려준다")
    void lockReturnsEmptyForNewVehicle() {
        vehicleRepository.save(new Vehicle(EXTERNAL_ID, SPEC));

        assertThat(vehicleRepository.lockByExternalId(new VehicleExternalId("vehicle-002")))
                .isEmpty();
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 재등록하면 차종·HW 버전·지역을 새 값으로 저장한다")
    void savesReregisteredSpec() {
        Vehicle vehicle = vehicleRepository.save(new Vehicle(EXTERNAL_ID, SPEC));

        vehicle.reregister(new VehicleSpec("model-b", "hw-2", "US"));
        entityManager.flush();

        Object[] row = (Object[]) entityManager
                .createNativeQuery("SELECT model, hw_version, region FROM vehicle WHERE id = :id")
                .setParameter("id", vehicle.id())
                .getSingleResult();
        assertThat(row).containsExactly("model-b", "hw-2", "US");
    }
}
