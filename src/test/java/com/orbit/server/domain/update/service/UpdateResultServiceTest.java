package com.orbit.server.domain.update.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        vehicleId = vehicle.id();
        ArtifactRelease release = new ArtifactRelease("model-a", "1.0.1");
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        CampaignCondition condition = new CampaignCondition(
                new CampaignTarget("model-a", null, null),
                VersionRange.unbounded(),
                new CampaignPeriod(FINISHED_AT, FINISHED_AT.plus(Duration.ofDays(1))));
        Campaign campaign = new Campaign(new CampaignName("campaign"), artifact.id(), condition);
        em.persist(campaign);
        campaignId = campaign.id();
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
    @DisplayName("UPDATE-POL-001: 실패 보고에 실패 사유가 없으면 거부한다")
    void rejectsFailedWithoutReason() {
        assertThatThrownBy(() -> service.record(vehicleId, reportOf(campaignId, UpdateStatus.FAILED, null)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-POL-001: 성공 보고에 실패 사유가 있으면 거부한다")
    void rejectsSucceededWithReason() {
        ReportUpdateResultCommand report = reportOf(campaignId, UpdateStatus.SUCCEEDED, FailureReason.DOWNLOAD_FAILED);

        assertThatThrownBy(() -> service.record(vehicleId, report)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-POL-001: 형식이 틀린 보고는 캠페인이 없어도 거부한다")
    void rejectsMalformedReportEvenForMissingCampaign() {
        assertThatThrownBy(() -> service.record(vehicleId, reportOf(Long.MAX_VALUE, UpdateStatus.FAILED, null)))
                .isInstanceOf(InvalidValueException.class);
    }

    private static ReportUpdateResultCommand reportOf(Long campaignId, UpdateStatus result, FailureReason reason) {
        String reasonName = reason == null ? null : reason.name();
        return new ReportUpdateResultCommand(campaignId, result.name(), reasonName, null, FINISHED_AT.toString());
    }

    // 엔티티에 getter 를 두지 않으므로 기록한 결과를 DB 에서 직접 읽는다
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
}
