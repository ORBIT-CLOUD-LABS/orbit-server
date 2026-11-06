package com.orbit.server.domain.monitoring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.monitoring.service.CampaignResultCounts;
import com.orbit.server.domain.monitoring.service.UnreportedVehicle;
import com.orbit.server.domain.monitoring.service.UpdateFailureReason;
import jakarta.persistence.EntityManager;
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
class CampaignProgressRepositoryTest {

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-10-02T00:00:00Z");

    @Autowired
    private CampaignProgressRepository campaignProgressRepository;

    // monitoring 은 테이블만 읽으므로 다른 컨텍스트의 엔티티 대신 행을 바로 넣는다
    @Autowired
    private EntityManager em;

    private Long campaignId;
    private Long otherCampaignId;

    @BeforeEach
    void setUp() {
        campaignId = insertCampaign("1.0.1");
        otherCampaignId = insertCampaign("1.0.2");
    }

    @Test
    @DisplayName("캠페인 진행 현황: 결과가 없으면 성공·실패가 0 이다")
    void countsZeroWithoutResults() {
        CampaignResultCounts counts = campaignProgressRepository.countResults(campaignId);

        assertThat(counts.succeeded()).isZero();
        assertThat(counts.failed()).isZero();
    }

    @Test
    @DisplayName("캠페인 진행 현황: 이 캠페인의 결과만 결과·실패 사유별로 센다")
    void countsResultsOfCampaignByReason() {
        insertResult(insertVehicle("vehicle-001", "model-a"), campaignId, "SUCCEEDED", null);
        insertResult(insertVehicle("vehicle-002", "model-a"), campaignId, "SUCCEEDED", null);
        insertResult(insertVehicle("vehicle-003", "model-a"), campaignId, "FAILED", "HASH_MISMATCH");
        insertResult(insertVehicle("vehicle-004", "model-a"), campaignId, "FAILED", "INSTALL_FAILED");
        insertResult(insertVehicle("vehicle-005", "model-a"), otherCampaignId, "FAILED", "INSTALL_FAILED");

        CampaignResultCounts counts = campaignProgressRepository.countResults(campaignId);

        assertThat(counts.succeeded()).isEqualTo(2);
        assertThat(counts.failed()).isEqualTo(2);
        assertThat(counts.failureReasons())
                .containsEntry(UpdateFailureReason.HASH_MISMATCH, 1L)
                .containsEntry(UpdateFailureReason.INSTALL_FAILED, 1L)
                .containsEntry(UpdateFailureReason.DOWNLOAD_FAILED, 0L);
    }

    @Test
    @DisplayName("캠페인 진행 현황: 같은 차종 중 이 캠페인에 결과를 보고하지 않은 차량만 후보로 고른다")
    void findsUnreportedVehiclesOfModel() {
        insertResult(insertVehicle("vehicle-001", "model-a"), campaignId, "SUCCEEDED", null);
        insertResult(insertVehicle("vehicle-002", "model-a"), otherCampaignId, "SUCCEEDED", null);
        insertVehicle("vehicle-003", "model-a");
        insertVehicle("vehicle-004", "model-b");

        assertThat(campaignProgressRepository.findUnreportedVehicles(campaignId, "model-a"))
                .containsExactlyInAnyOrder(
                        new UnreportedVehicle("model-a", "hw-1", "KR", "1.0.0"),
                        new UnreportedVehicle("model-a", "hw-1", "KR", "1.0.0"));
    }

    @Test
    @DisplayName("캠페인 진행 현황: 현재 상태가 없는 차량은 현재 버전을 몰라 후보에서 뺀다")
    void skipsVehicleWithoutState() {
        em.createNativeQuery("INSERT INTO vehicle (external_id, model, hw_version, region)"
                        + " VALUES ('vehicle-001', 'model-a', 'hw-1', 'KR')")
                .executeUpdate();

        assertThat(campaignProgressRepository.findUnreportedVehicles(campaignId, "model-a"))
                .isEmpty();
    }

    private Long insertCampaign(String version) {
        em.createNativeQuery("INSERT INTO artifact (model, version, sha256, size_bytes, path)"
                        + " VALUES ('model-a', :version, :sha256, 1, :path)")
                .setParameter("version", version)
                .setParameter("sha256", "a".repeat(64))
                .setParameter("path", "/firmware/model-a/" + version)
                .executeUpdate();
        em.createNativeQuery("INSERT INTO campaign (artifact_id, name, model, start_at, end_at, status)"
                        + " VALUES (LAST_INSERT_ID(), 'campaign', 'model-a', :startAt, :endAt, 'ACTIVE')")
                .setParameter("startAt", START)
                .setParameter("endAt", END)
                .executeUpdate();
        return lastInsertId();
    }

    // 차량 등록과 같이 차량과 상태를 함께 만든다
    private Long insertVehicle(String externalId, String model) {
        em.createNativeQuery("INSERT INTO vehicle (external_id, model, hw_version, region)"
                        + " VALUES (:externalId, :model, 'hw-1', 'KR')")
                .setParameter("externalId", externalId)
                .setParameter("model", model)
                .executeUpdate();
        Long vehicleId = lastInsertId();
        em.createNativeQuery("INSERT INTO vehicle_state (vehicle_id, current_version) VALUES (:vehicleId, '1.0.0')")
                .setParameter("vehicleId", vehicleId)
                .executeUpdate();
        return vehicleId;
    }

    private void insertResult(Long vehicleId, Long campaignId, String result, String failureReason) {
        em.createNativeQuery("INSERT INTO update_result"
                        + " (vehicle_id, campaign_id, result, failure_reason, finished_at)"
                        + " VALUES (:vehicleId, :campaignId, :result, :failureReason, :finishedAt)")
                .setParameter("vehicleId", vehicleId)
                .setParameter("campaignId", campaignId)
                .setParameter("result", result)
                .setParameter("failureReason", failureReason)
                .setParameter("finishedAt", FINISHED_AT)
                .executeUpdate();
    }

    private Long lastInsertId() {
        return ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }
}
