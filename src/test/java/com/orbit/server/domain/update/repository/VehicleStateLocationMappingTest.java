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

/** vehicle_state 의 위치 컬럼을 엔티티로 읽는지 검증한다. 위치는 체크인 쿼리만 쓰므로 여기서는 SQL 로 넣는다. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class VehicleStateLocationMappingTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T12:00:00Z");
    private static final Duration MAX_AGE = Duration.ofMinutes(10);

    @Autowired
    private EntityManager em;

    private Long vehicleId;

    @BeforeEach
    void setUp() {
        Vehicle vehicle = new Vehicle(new VehicleExternalId("vehicle-001"), new VehicleSpec("model-a", "hw-1", "KR"));
        em.persist(vehicle);
        vehicleId = vehicle.id();
        em.persist(new VehicleState(vehicleId, new SoftwareVersion("1.0.0")));
        em.flush();
    }

    @Test
    @DisplayName("위치를 받은 적 없으면 위치가 없다")
    void hasNoLocationBeforeFirstReport() {
        assertThat(reload().freshLocation(UPDATED_AT, MAX_AGE)).isEmpty();
    }

    @Test
    @DisplayName("유효 시간 안의 위치는 저장한 좌표 그대로 읽는다")
    void readsFreshLocation() {
        storeLocation("37.566500", "126.978000");

        assertThat(reload().freshLocation(UPDATED_AT.plus(MAX_AGE), MAX_AGE))
                .contains(new VehicleLocation(37.5665, 126.978));
    }

    @Test
    @DisplayName("유효 시간보다 오래된 위치는 쓰지 않는다")
    void ignoresStaleLocation() {
        storeLocation("37.566500", "126.978000");

        assertThat(reload().freshLocation(UPDATED_AT.plus(MAX_AGE).plusMillis(1), MAX_AGE))
                .isEmpty();
    }

    private void storeLocation(String latitude, String longitude) {
        em.createNativeQuery("UPDATE vehicle_state SET latitude = " + latitude + ", longitude = " + longitude
                        + ", location_updated_at = '2026-10-01 12:00:00' WHERE vehicle_id = :id")
                .setParameter("id", vehicleId)
                .executeUpdate();
    }

    private VehicleState reload() {
        em.clear();
        return em.find(VehicleState.class, vehicleId);
    }
}
