package com.orbit.server.domain.update.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactFile;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.entity.CampaignCondition;
import com.orbit.server.domain.campaign.entity.CampaignName;
import com.orbit.server.domain.campaign.entity.CampaignPeriod;
import com.orbit.server.domain.campaign.entity.CampaignTarget;
import com.orbit.server.domain.campaign.entity.VersionRange;
import com.orbit.server.domain.update.entity.FailureReason;
import com.orbit.server.domain.update.entity.UpdateStatus;
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
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
class UpdateResultServiceTest {

    private static final Instant FINISHED_AT = Instant.parse("2026-10-01T12:00:00Z");

    @Autowired
    private UpdateResultService service;

    @Autowired
    private EntityManager em;

    private Long vehicleId;
    private Long campaignId;

    @BeforeEach
    void setUp() {
        Vehicle vehicle = new Vehicle(new VehicleExternalId("vehicle-001"), new VehicleSpec("model-a", "hw-1", "KR"));
        em.persist(vehicle);
        vehicleId = vehicle.getId();
        ArtifactRelease release = new ArtifactRelease("model-a", "1.0.1");
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        CampaignCondition condition = new CampaignCondition(
                new CampaignTarget("model-a", null, null),
                VersionRange.unbounded(),
                new CampaignPeriod(FINISHED_AT, FINISHED_AT.plus(Duration.ofDays(1))));
        Campaign campaign = new Campaign(new CampaignName("campaign"), artifact.getId(), condition);
        em.persist(campaign);
        campaignId = campaign.getId();
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 성공 보고를 기록한다")
    void recordsSucceeded() {
        service.record(vehicleId, reportOf(campaignId, UpdateStatus.SUCCEEDED, null));

        assertThat(results()).containsExactly("SUCCEEDED:null");
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 실패 보고를 실패 사유와 함께 기록한다")
    void recordsFailed() {
        service.record(vehicleId, reportOf(campaignId, UpdateStatus.FAILED, FailureReason.INSTALL_FAILED));

        assertThat(results()).containsExactly("FAILED:INSTALL_FAILED");
    }

    @Test
    @DisplayName("UPDATE-POL-001: 같은 보고가 다시 와도 결과는 하나다")
    void recordsDuplicateReportOnce() {
        ReportUpdateResultCommand report = reportOf(campaignId, UpdateStatus.FAILED, FailureReason.HASH_MISMATCH);

        service.record(vehicleId, report);
        service.record(vehicleId, report);

        assertThat(results()).containsExactly("FAILED:HASH_MISMATCH");
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 없는 캠페인의 결과는 차량이 체크인을 계속할 수 있도록 오류 없이 버린다")
    void discardsResultOfMissingCampaign() {
        service.record(vehicleId, reportOf(Long.MAX_VALUE, UpdateStatus.SUCCEEDED, null));

        assertThat(allResults()).isEmpty();
    }

    @Test
    @DisplayName("UPDATE-POL-001: 실패 보고에 실패 사유가 없으면 차량이 체크인을 계속할 수 있도록 오류 없이 버린다")
    void discardsFailedWithoutReason() {
        service.record(vehicleId, reportOf(campaignId, UpdateStatus.FAILED, null));

        assertThat(allResults()).isEmpty();
    }

    @Test
    @DisplayName("UPDATE-POL-001: 성공 보고에 실패 사유가 있으면 오류 없이 버린다")
    void discardsSucceededWithReason() {
        service.record(vehicleId, reportOf(campaignId, UpdateStatus.SUCCEEDED, FailureReason.DOWNLOAD_FAILED));

        assertThat(allResults()).isEmpty();
    }

    @Test
    @DisplayName("UPDATE-POL-001: 성공 보고에 실패 상세가 있으면 조용히 저장하지 않고 오류 없이 버린다")
    void discardsSucceededWithDetail() {
        service.record(
                vehicleId,
                new ReportUpdateResultCommand(campaignId, "SUCCEEDED", null, "detail", FINISHED_AT.toString(), null));

        assertThat(allResults()).isEmpty();
    }

    @Test
    @DisplayName("결과를 기록하면 업데이트 시도 이력도 남긴다")
    void recordsAttemptHistory() {
        service.record(vehicleId, reportOf(campaignId, UpdateStatus.FAILED, FailureReason.HASH_MISMATCH));

        assertThat(events()).containsExactly("FAILED:HASH_MISMATCH");
    }

    @Test
    @DisplayName("같은 보고가 다시 와도 시도 이력은 하나다")
    void recordsDuplicateAttemptOnce() {
        ReportUpdateResultCommand report = reportOf(campaignId, UpdateStatus.FAILED, FailureReason.HASH_MISMATCH);

        service.record(vehicleId, report);
        service.record(vehicleId, report);

        assertThat(events()).containsExactly("FAILED:HASH_MISMATCH");
    }

    @Test
    @DisplayName("실패 후 재시도로 성공하면 결과는 덮어쓰지만 시도 이력에는 실패가 남는다")
    void keepsFailedAttemptAfterRetry() {
        service.record(vehicleId, reportOf(campaignId, UpdateStatus.FAILED, FailureReason.HASH_MISMATCH));

        service.record(
                vehicleId,
                new ReportUpdateResultCommand(
                        campaignId,
                        "SUCCEEDED",
                        null,
                        null,
                        FINISHED_AT.plusSeconds(60).toString(),
                        null));

        assertThat(results()).containsExactly("SUCCEEDED:null");
        assertThat(events()).containsExactly("FAILED:HASH_MISMATCH", "SUCCEEDED:null");
    }

    @Test
    @DisplayName("버린 보고는 시도 이력에도 남기지 않는다")
    void discardsAttemptOfRejectedReport() {
        service.record(vehicleId, reportOf(Long.MAX_VALUE, UpdateStatus.SUCCEEDED, null));
        service.record(vehicleId, reportOf(campaignId, UpdateStatus.FAILED, null));

        assertThat(allEvents()).isEmpty();
    }

    private static ReportUpdateResultCommand reportOf(Long campaignId, UpdateStatus result, FailureReason reason) {
        String reasonName = reason == null ? null : reason.name();
        return new ReportUpdateResultCommand(campaignId, result.name(), reasonName, null, FINISHED_AT.toString(), null);
    }

    // 영속성 컨텍스트가 아닌 DB 에 실제로 기록된 결과를 확인한다
    @SuppressWarnings("unchecked")
    private List<String> results() {
        return em.createNativeQuery("SELECT CONCAT(result, ':', IFNULL(failure_reason, 'null')) FROM update_result"
                        + " WHERE vehicle_id = :vehicleId AND campaign_id = :campaignId")
                .setParameter("vehicleId", vehicleId)
                .setParameter("campaignId", campaignId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object> allResults() {
        return em.createNativeQuery("SELECT id FROM update_result").getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<String> events() {
        return em.createNativeQuery("SELECT CONCAT(result, ':', IFNULL(failure_reason, 'null')) FROM vehicle_event"
                        + " WHERE vehicle_id = :vehicleId AND campaign_id = :campaignId ORDER BY finished_at")
                .setParameter("vehicleId", vehicleId)
                .setParameter("campaignId", campaignId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object> allEvents() {
        return em.createNativeQuery("SELECT id FROM vehicle_event").getResultList();
    }
}
