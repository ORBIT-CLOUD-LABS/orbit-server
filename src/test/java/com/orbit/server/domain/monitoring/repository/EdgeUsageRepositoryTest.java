package com.orbit.server.domain.monitoring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.monitoring.service.EdgeDownloadCount;
import com.orbit.server.domain.monitoring.service.EdgeVehicleCount;
import jakarta.persistence.EntityManager;
import java.time.Instant;
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

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-10-02T00:00:00Z");

    // ck_update_result_failure 가 실패한 결과에 실패 사유를 요구한다
    private static final String FAILED = "FAILED";

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

    @Test
    @DisplayName("Edge·캠페인별 다운로드 수: 결과가 없으면 빈 목록이다")
    void countsNoDownloadsWithoutResults() {
        assertThat(edgeUsageRepository.findDownloadCounts()).isEmpty();
    }

    @Test
    @DisplayName("Edge·캠페인별 다운로드 수: 파일을 받은 Edge 와 캠페인별로 성공·실패를 세고 Edge 안에서 campaignId 순으로 돌려준다")
    void countsDownloadsByEdgeAndCampaign() {
        Long campaignId = insertCampaign("1.0.1", "ACTIVE");
        Long laterCampaignId = insertCampaign("1.0.2", "ACTIVE");
        insertResult(insertVehicle("vehicle-001", null), laterCampaignId, "SUCCEEDED", "edge-seoul");
        insertResult(insertVehicle("vehicle-002", null), campaignId, "SUCCEEDED", "edge-seoul");
        insertResult(insertVehicle("vehicle-003", null), campaignId, "SUCCEEDED", "edge-seoul");
        insertResult(insertVehicle("vehicle-004", null), campaignId, "FAILED", "edge-seoul");
        insertResult(insertVehicle("vehicle-005", null), campaignId, "FAILED", "edge-busan");

        assertThat(edgeUsageRepository.findDownloadCounts())
                .containsExactly(
                        new EdgeDownloadCount("edge-busan", campaignId, 0, 1),
                        new EdgeDownloadCount("edge-seoul", campaignId, 2, 1),
                        new EdgeDownloadCount("edge-seoul", laterCampaignId, 1, 0));
    }

    @Test
    @DisplayName("Edge·캠페인별 다운로드 수: 파일을 받은 Edge 가 없는 결과는 어느 Edge 에도 세지 않는다")
    void skipsResultWithoutEdge() {
        Long campaignId = insertCampaign("1.0.1", "ACTIVE");
        insertResult(insertVehicle("vehicle-001", null), campaignId, "SUCCEEDED", "edge-seoul");
        insertResult(insertVehicle("vehicle-002", null), campaignId, "FAILED", null);

        assertThat(edgeUsageRepository.findDownloadCounts())
                .containsExactly(new EdgeDownloadCount("edge-seoul", campaignId, 1, 0));
    }

    @Test
    @DisplayName("Edge·캠페인별 다운로드 수: 비활성 캠페인의 결과도 센다")
    void countsDownloadsOfInactiveCampaign() {
        Long campaignId = insertCampaign("1.0.1", "INACTIVE");
        insertResult(insertVehicle("vehicle-001", null), campaignId, "SUCCEEDED", "edge-seoul");

        assertThat(edgeUsageRepository.findDownloadCounts())
                .containsExactly(new EdgeDownloadCount("edge-seoul", campaignId, 1, 0));
    }

    private Long insertCampaign(String version, String status) {
        em.createNativeQuery("INSERT INTO artifact (model, version, sha256, size_bytes, path)"
                        + " VALUES ('model-a', :version, :sha256, 1, :path)")
                .setParameter("version", version)
                .setParameter("sha256", "a".repeat(64))
                .setParameter("path", "/firmware/model-a/" + version)
                .executeUpdate();
        em.createNativeQuery("INSERT INTO campaign (artifact_id, name, model, start_at, end_at, status)"
                        + " VALUES (LAST_INSERT_ID(), 'campaign', 'model-a', :startAt, :endAt, :status)")
                .setParameter("startAt", START)
                .setParameter("endAt", END)
                .setParameter("status", status)
                .executeUpdate();
        return lastInsertId();
    }

    private void insertResult(Long vehicleId, Long campaignId, String result, String edgeId) {
        em.createNativeQuery("INSERT INTO update_result"
                        + " (vehicle_id, campaign_id, result, failure_reason, edge_id, finished_at)"
                        + " VALUES (:vehicleId, :campaignId, :result, :failureReason, :edgeId, :finishedAt)")
                .setParameter("vehicleId", vehicleId)
                .setParameter("campaignId", campaignId)
                .setParameter("result", result)
                .setParameter("failureReason", FAILED.equals(result) ? "DOWNLOAD_FAILED" : null)
                .setParameter("edgeId", edgeId)
                .setParameter("finishedAt", FINISHED_AT)
                .executeUpdate();
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
