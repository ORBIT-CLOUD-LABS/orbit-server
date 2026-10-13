package com.orbit.server.domain.update.repository;

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
import com.orbit.server.domain.update.entity.UpdateFailure;
import com.orbit.server.domain.update.entity.UpdateOutcome;
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
class UpdateResultJpaRepositoryTest {

    private static final Instant FINISHED_AT = Instant.parse("2026-10-01T12:00:00Z");
    private static final UpdateOutcome SUCCEEDED = new UpdateOutcome(UpdateStatus.SUCCEEDED, null, FINISHED_AT);
    private static final UpdateOutcome FAILED = new UpdateOutcome(
            UpdateStatus.FAILED, new UpdateFailure(FailureReason.HASH_MISMATCH, "sha256 mismatch"), FINISHED_AT);

    @Autowired
    private UpdateResultRepository updateResultRepository;

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
    @DisplayName("UPDATE-REQ-001: 성공 결과를 실패 정보 없이 기록한다")
    void recordsSucceeded() {
        updateResultRepository.record(vehicleId, campaignId, SUCCEEDED);

        assertThat(results()).containsExactly("SUCCEEDED:null:null:2026-10-01 12:00:00");
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 실패 결과를 실패 사유·상세와 함께 기록한다")
    void recordsFailed() {
        updateResultRepository.record(vehicleId, campaignId, FAILED);

        assertThat(results()).containsExactly("FAILED:HASH_MISMATCH:sha256 mismatch:2026-10-01 12:00:00");
    }

    @Test
    @DisplayName("UPDATE-POL-001: 같은 차량·캠페인의 결과는 마지막 보고로 덮어쓴다")
    void overwritesWithLatestReport() {
        updateResultRepository.record(vehicleId, campaignId, FAILED);

        updateResultRepository.record(
                vehicleId, campaignId, new UpdateOutcome(UpdateStatus.SUCCEEDED, null, FINISHED_AT.plusSeconds(60)));

        assertThat(results()).containsExactly("SUCCEEDED:null:null:2026-10-01 12:01:00");
    }

    @Test
    @DisplayName("UPDATE-POL-001: 같은 결과를 다시 보고해도 결과는 하나다")
    void recordsSameReportIdempotently() {
        updateResultRepository.record(vehicleId, campaignId, FAILED);

        updateResultRepository.record(vehicleId, campaignId, FAILED);

        assertThat(results()).containsExactly("FAILED:HASH_MISMATCH:sha256 mismatch:2026-10-01 12:00:00");
    }

    // 엔티티에 getter 를 두지 않으므로 기록한 결과를 DB 에서 직접 읽는다. 세션 시간대는 UTC 로 고정돼 있다
    @SuppressWarnings("unchecked")
    private List<String> results() {
        return em.createNativeQuery("SELECT CONCAT_WS(':', result, IFNULL(failure_reason, 'null'),"
                        + " IFNULL(failure_detail, 'null'), DATE_FORMAT(finished_at, '%Y-%m-%d %H:%i:%s'))"
                        + " FROM update_result WHERE vehicle_id = :vehicleId AND campaign_id = :campaignId")
                .setParameter("vehicleId", vehicleId)
                .setParameter("campaignId", campaignId)
                .getResultList();
    }
}
