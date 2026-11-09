package com.orbit.server.domain.update.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.update.entity.SoftwareVersion;
import com.orbit.server.domain.update.entity.VehicleLocation;
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.repository.VehicleRepository;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
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

    private static final VehicleLocation SEOUL = new VehicleLocation(37.5665, 126.978);
    private static final Duration MAX_AGE = Duration.ofMinutes(10);

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
        vehicleId = vehicleRepository.save(vehicle).getId();
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

    @Test
    @DisplayName("VEHICLE-REQ-001: 체크인하면 현재 버전과 마지막 체크인 시각을 덮어쓴다")
    void recordsCheckIn() {
        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.0");

        vehicleStateService.recordCheckIn(vehicleId, "1.0.1");

        assertThat(currentVersions()).containsExactly("1.0.1");
        assertThat(lastSeenRecorded()).isTrue();
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 상태가 없는 차량의 체크인은 데이터가 어긋난 것이므로 서버 오류다")
    void failsCheckInWithoutState() {
        assertThatThrownBy(() -> vehicleStateService.recordCheckIn(vehicleId, "1.0.0"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 체크인한 현재 버전이 비어 있으면 거부한다")
    void rejectsBlankCheckInVersion() {
        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.0");

        assertThatThrownBy(() -> vehicleStateService.recordCheckIn(vehicleId, " "))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 마지막 체크인 때 기록한 현재 버전을 조회한다")
    void getsCurrentVersionRecordedAtLastCheckIn() {
        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.0");
        vehicleStateService.recordCheckIn(vehicleId, "1.0.1");

        assertThat(vehicleStateService.getCurrentVersion(vehicleId)).isEqualTo(new SoftwareVersion("1.0.1"));
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 상태가 없는 차량의 현재 버전 조회는 데이터가 어긋난 것이므로 서버 오류다")
    void failsGettingCurrentVersionWithoutState() {
        assertThatThrownBy(() -> vehicleStateService.getCurrentVersion(vehicleId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 유효 시간 안에 받은 마지막 위치를 조회한다")
    void findsFreshLocation() {
        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.0");
        vehicleStateService.recordLocation(vehicleId, SEOUL);
        clearPersistenceContext();

        assertThat(vehicleStateService.findFreshLocation(vehicleId, databaseNow(), MAX_AGE))
                .contains(SEOUL);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 위치를 받은 적이 없거나 유효 시간이 지났으면 빈 값이다")
    void returnsEmptyWithoutFreshLocation() {
        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.0");
        assertThat(vehicleStateService.findFreshLocation(vehicleId, databaseNow(), MAX_AGE))
                .isEmpty();

        vehicleStateService.recordLocation(vehicleId, SEOUL);
        clearPersistenceContext();
        Instant afterMaxAge = databaseNow().plus(MAX_AGE).plusSeconds(1);

        assertThat(vehicleStateService.findFreshLocation(vehicleId, afterMaxAge, MAX_AGE))
                .isEmpty();
    }

    // 위치는 native UPDATE 로 기록되어 같은 영속성 컨텍스트에 남은 엔티티에는 반영되지 않는다.
    // 실제 매니페스트 요청은 새 트랜잭션에서 읽으므로, 테스트도 캐시를 비워 DB 값을 읽게 한다
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 매니페스트가 고른 Edge 를 기록하고 새 Edge 로 덮어쓴다")
    void assignsEdge() {
        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.0");

        vehicleStateService.assignEdge(vehicleId, "edge-seoul");
        assertThat(assignedEdgeId()).isEqualTo("edge-seoul");

        vehicleStateService.assignEdge(vehicleId, "edge-busan");
        assertThat(assignedEdgeId()).isEqualTo("edge-busan");
    }

    @Test
    @DisplayName("Edge id 가 비어 있으면 거부하고, 상태가 없는 차량이면 서버 오류다")
    void rejectsInvalidEdgeAssignment() {
        assertThatThrownBy(() -> vehicleStateService.assignEdge(vehicleId, "edge-seoul"))
                .isInstanceOf(IllegalStateException.class);

        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.0");

        assertThatThrownBy(() -> vehicleStateService.assignEdge(vehicleId, " "))
                .isInstanceOf(InvalidValueException.class);
    }

    private String assignedEdgeId() {
        entityManager.flush();
        return (String) entityManager
                .createNativeQuery("SELECT assigned_edge_id FROM vehicle_state WHERE vehicle_id = :vehicleId")
                .setParameter("vehicleId", vehicleId)
                .getSingleResult();
    }

    private Instant databaseNow() {
        return (Instant)
                entityManager.createNativeQuery("SELECT NOW(3)", Instant.class).getSingleResult();
    }

    private boolean lastSeenRecorded() {
        Object recorded = entityManager
                .createNativeQuery("SELECT last_seen_at IS NOT NULL FROM vehicle_state WHERE vehicle_id = :vehicleId")
                .setParameter("vehicleId", vehicleId)
                .getSingleResult();
        return ((Number) recorded).intValue() == 1;
    }

    // 영속성 컨텍스트가 아닌 DB 에 실제로 저장된 버전을 확인한다
    @SuppressWarnings("unchecked")
    private List<String> currentVersions() {
        entityManager.flush();
        return entityManager
                .createNativeQuery("SELECT current_version FROM vehicle_state WHERE vehicle_id = :vehicleId")
                .setParameter("vehicleId", vehicleId)
                .getResultList();
    }
}
