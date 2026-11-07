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
class VehicleEventRepositoryTest {

    private static final Instant FINISHED_AT = Instant.parse("2026-10-01T12:00:00Z");
    private static final UpdateOutcome FAILED = new UpdateOutcome(
            UpdateStatus.FAILED,
            new UpdateFailure(FailureReason.HASH_MISMATCH, "sha256 mismatch"),
            FINISHED_AT,
            "edge-seoul");
    private static final UpdateOutcome RETRY_SUCCEEDED =
            new UpdateOutcome(UpdateStatus.SUCCEEDED, null, FINISHED_AT.plusSeconds(60), null);

    @Autowired
    private VehicleEventRepository vehicleEventRepository;

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
    @DisplayName("업데이트 시도를 결과·실패 정보·받은 Edge 와 함께 남긴다")
    void recordsAttempt() {
        vehicleEventRepository.record(vehicleId, campaignId, FAILED);

        assertThat(events()).containsExactly("FAILED:HASH_MISMATCH:sha256 mismatch:edge-seoul:2026-10-01 12:00:00");
    }

    @Test
    @DisplayName("UPDATE-POL-001: 같은 시도를 다시 보고해도 이력은 하나다")
    void ignoresResentAttempt() {
        vehicleEventRepository.record(vehicleId, campaignId, FAILED);

        vehicleEventRepository.record(vehicleId, campaignId, FAILED);

        assertThat(events()).containsExactly("FAILED:HASH_MISMATCH:sha256 mismatch:edge-seoul:2026-10-01 12:00:00");
    }

    @Test
    @DisplayName("실패 후 재시도한 결과는 실패 이력을 지우지 않고 따로 남긴다")
    void keepsFailureBeforeRetry() {
        vehicleEventRepository.record(vehicleId, campaignId, FAILED);

        vehicleEventRepository.record(vehicleId, campaignId, RETRY_SUCCEEDED);

        assertThat(events())
                .containsExactly(
                        "FAILED:HASH_MISMATCH:sha256 mismatch:edge-seoul:2026-10-01 12:00:00",
                        "SUCCEEDED:null:null:null:2026-10-01 12:01:00");
    }

    // 영속성 컨텍스트가 아닌 DB 에 실제로 기록된 이력을 확인한다. 세션 시간대는 UTC 로 고정돼 있다
    @SuppressWarnings("unchecked")
    private List<String> events() {
        return em.createNativeQuery("SELECT CONCAT_WS(':', result, IFNULL(failure_reason, 'null'),"
                        + " IFNULL(failure_detail, 'null'), IFNULL(edge_id, 'null'),"
                        + " DATE_FORMAT(finished_at, '%Y-%m-%d %H:%i:%s'))"
                        + " FROM vehicle_event WHERE vehicle_id = :vehicleId AND campaign_id = :campaignId"
                        + " ORDER BY finished_at")
                .setParameter("vehicleId", vehicleId)
                .setParameter("campaignId", campaignId)
                .getResultList();
    }
}
