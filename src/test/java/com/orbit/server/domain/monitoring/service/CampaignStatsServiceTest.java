package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.global.error.BusinessException;
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
class CampaignStatsServiceTest {

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-10-02T00:00:00Z");

    @Autowired
    private CampaignStatsService campaignStatsService;

    // monitoring 은 테이블만 읽으므로 다른 컨텍스트의 엔티티 대신 행을 바로 넣는다
    @Autowired
    private EntityManager em;

    private Long campaignId;

    // 목표 버전 1.0.1, 대상 model-a / hw-1, 현재 버전 1.0.0 ~ 1.0.9
    @BeforeEach
    void setUp() {
        campaignId = insertCampaign();
    }

    @Test
    @DisplayName("캠페인 진행 현황: 결과도 대상 차량도 없으면 모두 0 이다")
    void countsZeroWithoutVehicles() {
        CampaignStats stats = campaignStatsService.getStats(campaignId);

        assertThat(stats.campaignId()).isEqualTo(campaignId);
        assertThat(stats.targetVehicles()).isZero();
        assertThat(stats.succeeded()).isZero();
        assertThat(stats.failed()).isZero();
        assertThat(stats.inProgress()).isZero();
        assertThat(stats.failureReasons()).hasSize(UpdateFailureReason.values().length);
    }

    @Test
    @DisplayName("캠페인 진행 현황: 성공·실패(사유별)·진행 중을 나누고 대상은 그 합이다")
    void countsResultDistribution() {
        // 성공해 현재 버전이 목표 버전이 된 차량도 대상에 든다
        insertResult(insertVehicle("succeeded-1", "hw-1", "1.0.1"), "SUCCEEDED", null);
        insertResult(insertVehicle("succeeded-2", "hw-1", "1.0.1"), "SUCCEEDED", null);
        insertResult(insertVehicle("failed-1", "hw-1", "1.0.0"), "FAILED", "DOWNLOAD_FAILED");
        insertResult(insertVehicle("failed-2", "hw-1", "1.0.0"), "FAILED", "INSTALL_FAILED");
        insertResult(insertVehicle("failed-3", "hw-1", "1.0.0"), "FAILED", "INSTALL_FAILED");
        insertVehicle("pending-1", "hw-1", "1.0.0");
        // 1.0 은 1.0.0 과 같아 목표 버전 1.0.1 보다 낮다
        insertVehicle("pending-2", "hw-1", "1.0");

        CampaignStats stats = campaignStatsService.getStats(campaignId);

        assertThat(stats.succeeded()).isEqualTo(2);
        assertThat(stats.failed()).isEqualTo(3);
        assertThat(stats.inProgress()).isEqualTo(2);
        assertThat(stats.targetVehicles()).isEqualTo(7);
        assertThat(stats.failureReasons())
                .contains(
                        entry(UpdateFailureReason.DOWNLOAD_FAILED, 1L),
                        entry(UpdateFailureReason.INSTALL_FAILED, 2L),
                        entry(UpdateFailureReason.HASH_MISMATCH, 0L));
    }

    @Test
    @DisplayName("캠페인 진행 현황: 결과를 보고하지 않았어도 조건 밖이거나 이미 목표 버전 이상인 차량은 진행 중이 아니다")
    void excludesUnreportedNonTargets() {
        insertVehicle("other-hw", "hw-9", "1.0.0");
        insertVehicle("out-of-range", "hw-1", "0.9.0");
        insertVehicle("already-installed", "hw-1", "1.0.1");
        // 버전 범위(1.0.0 ~ 1.0.9) 안이지만 목표 버전 1.0.1 보다 높다
        insertVehicle("above-target", "hw-1", "1.0.5");

        CampaignStats stats = campaignStatsService.getStats(campaignId);

        assertThat(stats.inProgress()).isZero();
        assertThat(stats.targetVehicles()).isZero();
    }

    @Test
    @DisplayName("캠페인 진행 현황: 비활성 캠페인도 끝내지 못한 차량을 진행 중으로 센다")
    void countsInProgressOfInactiveCampaign() {
        insertVehicle("pending-1", "hw-1", "1.0.0");
        em.createNativeQuery("UPDATE campaign SET status = 'INACTIVE' WHERE id = :id")
                .setParameter("id", campaignId)
                .executeUpdate();

        assertThat(campaignStatsService.getStats(campaignId).inProgress()).isEqualTo(1);
    }

    @Test
    @DisplayName("캠페인 진행 현황: 캠페인이 없으면 CAMPAIGN_NOT_FOUND")
    void rejectsMissingCampaign() {
        assertThatThrownBy(() -> campaignStatsService.getStats(Long.MAX_VALUE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CampaignErrorCode.CAMPAIGN_NOT_FOUND);
    }

    private Long insertCampaign() {
        em.createNativeQuery("INSERT INTO artifact (model, version, sha256, size_bytes, path)"
                        + " VALUES ('model-a', '1.0.1', :sha256, 1, '/firmware/model-a/1.0.1')")
                .setParameter("sha256", "a".repeat(64))
                .executeUpdate();
        em.createNativeQuery("INSERT INTO campaign"
                        + " (artifact_id, name, model, current_version_min, current_version_max,"
                        + " start_at, end_at, status)"
                        + " VALUES (LAST_INSERT_ID(), 'campaign', 'model-a', '1.0.0', '1.0.9',"
                        + " :startAt, :endAt, 'ACTIVE')")
                .setParameter("startAt", START)
                .setParameter("endAt", END)
                .executeUpdate();
        Long id = lastInsertId();
        em.createNativeQuery("INSERT INTO campaign_target_hw_version (campaign_id, hw_version) VALUES (:id, 'hw-1')")
                .setParameter("id", id)
                .executeUpdate();
        return id;
    }

    // 차량 등록과 같이 차량과 상태를 함께 만든다
    private Long insertVehicle(String externalId, String hwVersion, String currentVersion) {
        em.createNativeQuery("INSERT INTO vehicle (external_id, model, hw_version, region)"
                        + " VALUES (:externalId, 'model-a', :hwVersion, 'KR')")
                .setParameter("externalId", externalId)
                .setParameter("hwVersion", hwVersion)
                .executeUpdate();
        Long vehicleId = lastInsertId();
        em.createNativeQuery("INSERT INTO vehicle_state (vehicle_id, current_version)"
                        + " VALUES (:vehicleId, :currentVersion)")
                .setParameter("vehicleId", vehicleId)
                .setParameter("currentVersion", currentVersion)
                .executeUpdate();
        return vehicleId;
    }

    private void insertResult(Long vehicleId, String result, String failureReason) {
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
