package com.orbit.server.domain.update.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.update.entity.VehicleLocation;
import com.orbit.server.domain.update.service.EdgeAssignment.FallbackUrl;
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.repository.VehicleRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * application-test.yml 의 Edge 3개(서울·대전·부산, 기본 Edge 서울)와 Edge 선택 규칙 기본값(fallback 2개, 위치 유효 시간 30분)을
 * 전제로 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class EdgeAssignerTest {

    private static final String PATH = "/firmware/model-a/1.0.1/firmware.bin";
    // 울산 근처: 부산 → 대전 → 서울 순으로 가깝다
    private static final VehicleLocation NEAR_BUSAN = new VehicleLocation(35.5384, 129.3114);

    @Autowired
    private EdgeAssigner edgeAssigner;

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
        vehicleStateService.recordCurrentVersion(vehicleId, "1.0.0");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 유효한 위치가 있으면 가까운 Edge 로 URL 을 만들고 다음 Edge 들을 예비로 둔다")
    void assignsNearestEdgeWithFallbacks() {
        vehicleStateService.recordLocation(vehicleId, NEAR_BUSAN);

        EdgeAssignment assignment = assign();

        assertThat(assignment.edge().edgeId()).isEqualTo("edge-busan");
        assertThat(assignment.edge().distanceKm()).hasValueCloseTo(50.0, within(5.0));
        assertThat(assignment.downloadUrl()).isEqualTo("http://cdn.test:30083" + PATH);
        assertThat(assignment.fallbackUrls())
                .containsExactly(
                        new FallbackUrl("edge-daejeon", "http://cdn.test:30082" + PATH),
                        new FallbackUrl("edge-seoul", "http://cdn.test:30081" + PATH));
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 위치가 없으면 기본 Edge 로 URL 을 만들고 거리는 비운다")
    void assignsDefaultEdgeWithoutLocation() {
        EdgeAssignment assignment = assign();

        assertThat(assignment.edge().edgeId()).isEqualTo("edge-seoul");
        assertThat(assignment.edge().distanceKm()).isEmpty();
        assertThat(assignment.downloadUrl()).isEqualTo("http://cdn.test:30081" + PATH);
        assertThat(assignment.fallbackUrls())
                .extracting(FallbackUrl::edgeId)
                .containsExactly("edge-daejeon", "edge-busan");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 위치가 유효 시간보다 오래됐으면 기본 Edge 를 고른다")
    void assignsDefaultEdgeWhenLocationIsStale() {
        vehicleStateService.recordLocation(vehicleId, NEAR_BUSAN);
        moveLocationUpdatedAtBack(Duration.ofMinutes(31));

        EdgeAssignment assignment = assign();

        assertThat(assignment.edge().edgeId()).isEqualTo("edge-seoul");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 고른 Edge 를 차량 상태의 배정 Edge 로 기록한다")
    void recordsAssignedEdge() {
        vehicleStateService.recordLocation(vehicleId, NEAR_BUSAN);

        assign();

        assertThat(assignedEdgeId()).isEqualTo("edge-busan");
    }

    private EdgeAssignment assign() {
        return edgeAssigner.assign(vehicleStateService.getManifestState(vehicleId), PATH);
    }

    private void moveLocationUpdatedAtBack(Duration duration) {
        entityManager
                .createNativeQuery(
                        "UPDATE vehicle_state SET location_updated_at = location_updated_at - INTERVAL :seconds"
                                + " SECOND WHERE vehicle_id = :vehicleId")
                .setParameter("seconds", duration.toSeconds())
                .setParameter("vehicleId", vehicleId)
                .executeUpdate();
    }

    private String assignedEdgeId() {
        return (String) entityManager
                .createNativeQuery("SELECT assigned_edge_id FROM vehicle_state WHERE vehicle_id = :vehicleId")
                .setParameter("vehicleId", vehicleId)
                .getSingleResult();
    }
}
