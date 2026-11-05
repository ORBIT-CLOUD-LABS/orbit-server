package com.orbit.server.domain.update.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.update.entity.SoftwareVersion;
import com.orbit.server.domain.update.entity.VehicleLocation;
import com.orbit.server.domain.update.entity.VehicleState;
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
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
class VehicleStateLocationRecordTest {

    private static final VehicleLocation SEOUL = new VehicleLocation(37.5665, 126.978);
    private static final VehicleLocation BUSAN = new VehicleLocation(35.1796, 129.0756);
    private static final Duration MAX_AGE = Duration.ofMinutes(10);

    @Autowired
    private VehicleStateRepository vehicleStateRepository;

    @Autowired
    private EntityManager em;

    private Long vehicleId;

    @BeforeEach
    void setUp() {
        Vehicle vehicle = new Vehicle(new VehicleExternalId("vehicle-001"), new VehicleSpec("model-a", "hw-1", "KR"));
        em.persist(vehicle);
        vehicleId = vehicle.getId();
        em.persist(new VehicleState(vehicleId, new SoftwareVersion("1.0.0")));
        em.flush();
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 차량 위치와 갱신 시각을 DB 시계로 기록한다")
    void recordsLocationWithDatabaseClock() {
        int updated = vehicleStateRepository.recordLocation(vehicleId, SEOUL);

        assertThat(updated).isEqualTo(1);
        assertThat(reload().freshLocation(databaseNow(), MAX_AGE)).contains(SEOUL);
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 새 위치로 이전 위치를 덮어쓴다")
    void overwritesPreviousLocation() {
        vehicleStateRepository.recordLocation(vehicleId, SEOUL);

        vehicleStateRepository.recordLocation(vehicleId, BUSAN);

        assertThat(reload().freshLocation(databaseNow(), MAX_AGE)).contains(BUSAN);
    }

    @Test
    @DisplayName("상태가 없는 차량이면 아무것도 기록하지 않는다")
    void recordsNothingWithoutState() {
        assertThat(vehicleStateRepository.recordLocation(vehicleId + 1, SEOUL)).isZero();
    }

    private Instant databaseNow() {
        return (Instant) em.createNativeQuery("SELECT NOW(3)", Instant.class).getSingleResult();
    }

    private VehicleState reload() {
        em.clear();
        return em.find(VehicleState.class, vehicleId);
    }
}
