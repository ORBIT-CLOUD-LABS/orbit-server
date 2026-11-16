package com.orbit.server.domain.monitoring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.monitoring.service.EdgeVehicleCount;
import jakarta.persistence.EntityManager;
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
class EdgeUsageRepositoryTest {

    @Autowired
    private EdgeUsageRepository edgeUsageRepository;

    // monitoring 은 테이블만 읽으므로 다른 컨텍스트의 엔티티 대신 행을 바로 넣는다
    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("Edge 별 배정 차량 수: 차량이 없으면 빈 목록이다")
    void countsNothingWithoutVehicles() {
        assertThat(edgeUsageRepository.findAssignedVehicleCounts()).isEmpty();
    }

    @Test
    @DisplayName("Edge 별 배정 차량 수: 배정된 Edge 별로 차량 수를 센다")
    void countsAssignedVehiclesByEdge() {
        insertVehicle("vehicle-001", "edge-seoul");
        insertVehicle("vehicle-002", "edge-seoul");
        insertVehicle("vehicle-003", "edge-busan");

        assertThat(edgeUsageRepository.findAssignedVehicleCounts())
                .containsExactlyInAnyOrder(
                        new EdgeVehicleCount("edge-seoul", 2), new EdgeVehicleCount("edge-busan", 1));
    }

    @Test
    @DisplayName("Edge 별 배정 차량 수: 배정된 Edge 가 없는 차량은 세지 않는다")
    void skipsVehicleWithoutAssignedEdge() {
        insertVehicle("vehicle-001", "edge-seoul");
        insertVehicle("vehicle-002", null);

        assertThat(edgeUsageRepository.findAssignedVehicleCounts())
                .containsExactly(new EdgeVehicleCount("edge-seoul", 1));
    }

    // 차량 등록과 같이 차량과 상태를 함께 만든다
    private Long insertVehicle(String externalId, String assignedEdgeId) {
        em.createNativeQuery("INSERT INTO vehicle (external_id, model, hw_version, region)"
                        + " VALUES (:externalId, 'model-a', 'hw-1', 'KR')")
                .setParameter("externalId", externalId)
                .executeUpdate();
        Long vehicleId = lastInsertId();
        em.createNativeQuery("INSERT INTO vehicle_state (vehicle_id, current_version, assigned_edge_id)"
                        + " VALUES (:vehicleId, '1.0.0', :assignedEdgeId)")
                .setParameter("vehicleId", vehicleId)
                .setParameter("assignedEdgeId", assignedEdgeId)
                .executeUpdate();
        return vehicleId;
    }

    private Long lastInsertId() {
        return ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }
}
