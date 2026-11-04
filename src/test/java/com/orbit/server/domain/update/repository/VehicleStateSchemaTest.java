package com.orbit.server.domain.update.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** [Phase2] 위치·배정 Edge 컬럼의 DB 제약을 엔티티 검증을 거치지 않고 SQL 로 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class VehicleStateSchemaTest {

    private static final String UPDATED_AT = "'2026-10-01 12:00:00'";

    @Autowired
    private EntityManager em;

    private Long vehicleId;

    @BeforeEach
    void setUp() {
        Vehicle vehicle = new Vehicle(new VehicleExternalId("vehicle-001"), new VehicleSpec("model-a", "hw-1", "KR"));
        em.persist(vehicle);
        vehicleId = vehicle.id();
        em.createNativeQuery("INSERT INTO vehicle_state (vehicle_id, current_version) VALUES (:id, '1.0.0')")
                .setParameter("id", vehicleId)
                .executeUpdate();
    }

    @Test
    @DisplayName("기존 상태 행은 위치와 배정 Edge 없이 그대로 쓸 수 있다")
    void allowsStateWithoutLocation() {
        assertThat(location()).isEqualTo("null:null:null:null");
    }

    @Test
    @DisplayName("위치·갱신 시각·배정 Edge 를 저장한다")
    void storesLocationAndAssignedEdge() {
        update("latitude = 37.566500, longitude = 126.978000, location_updated_at = " + UPDATED_AT
                + ", assigned_edge_id = 'edge-seoul'");

        assertThat(location()).isEqualTo("37.566500:126.978000:2026-10-01 12:00:00:edge-seoul");
    }

    @Test
    @DisplayName("위도가 -90~90 을 벗어나면 저장하지 않는다")
    void rejectsLatitudeOutOfRange() {
        assertThatThrownBy(() -> update("latitude = 90.000001, longitude = 0, location_updated_at = " + UPDATED_AT))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("경도가 -180~180 을 벗어나면 저장하지 않는다")
    void rejectsLongitudeOutOfRange() {
        assertThatThrownBy(() -> update("latitude = 0, longitude = -180.000001, location_updated_at = " + UPDATED_AT))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("위도·경도·갱신 시각 중 일부만 있으면 저장하지 않는다")
    void rejectsPartialLocation() {
        assertThatThrownBy(() -> update("latitude = 37.5665, longitude = 126.978"))
                .isInstanceOf(PersistenceException.class);
    }

    private void update(String assignments) {
        em.createNativeQuery("UPDATE vehicle_state SET " + assignments + " WHERE vehicle_id = :id")
                .setParameter("id", vehicleId)
                .executeUpdate();
    }

    private String location() {
        Object[] row =
                // 시각은 JVM 시간대로 바뀌지 않도록 DB 에서 문자열로 읽는다
                (Object[]) em.createNativeQuery("SELECT latitude, longitude,"
                                + " DATE_FORMAT(location_updated_at, '%Y-%m-%d %H:%i:%s'), assigned_edge_id"
                                + " FROM vehicle_state WHERE vehicle_id = :id")
                        .setParameter("id", vehicleId)
                        .getSingleResult();
        return row[0] + ":" + row[1] + ":" + row[2] + ":" + row[3];
    }
}
