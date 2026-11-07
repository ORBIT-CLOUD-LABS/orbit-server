package com.orbit.server.domain.monitoring.repository;

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
import com.orbit.server.domain.monitoring.service.VehicleUpdateAttempt;
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
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class VehicleUpdateHistoryRepositoryTest {

    private static final Instant START_AT = Instant.parse("2026-10-01T00:00:00Z");
    private static final Limit ALL = Limit.of(10);

    @Autowired
    private VehicleUpdateHistoryRepository repository;

    @Autowired
    private EntityManager em;

    private Long vehicleKey;
    private Long otherVehicleKey;
    private Long campaign101;
    private Long campaign102;

    @BeforeEach
    void setUp() {
        vehicleKey = persistVehicle("vehicle-001");
        otherVehicleKey = persistVehicle("vehicle-002");
        campaign101 = persistCampaign("1.0.1");
        campaign102 = persistCampaign("1.0.2");
    }

    @Test
    @DisplayName("차량이 보내는 식별자로 차량 ID 를 찾는다")
    void findsVehicleKey() {
        assertThat(repository.findVehicleKey("vehicle-001")).contains(vehicleKey);
    }

    @Test
    @DisplayName("없는 차량이면 빈 값이다")
    void returnsEmptyForMissingVehicle() {
        assertThat(repository.findVehicleKey("missing")).isEmpty();
    }

    @Test
    @DisplayName("업데이트 시도를 캠페인이 배포한 버전과 함께 완료 시각 역순으로 조회한다")
    void findsAttemptsWithVersionLatestFirst() {
        insertEvent(vehicleKey, campaign101, "SUCCEEDED", null, "2026-10-01 10:00:00");
        insertEvent(vehicleKey, campaign102, "FAILED", "HASH_MISMATCH", "2026-10-02 10:00:00");
        insertEvent(vehicleKey, campaign102, "SUCCEEDED", null, "2026-10-02 11:00:00");

        List<VehicleUpdateAttempt> attempts = repository.findRecentAttempts(vehicleKey, ALL);

        assertThat(attempts)
                .extracting(attempt -> attempt.version() + ":" + attempt.result() + ":" + attempt.failureReason())
                .containsExactly("1.0.2:SUCCEEDED:null", "1.0.2:FAILED:HASH_MISMATCH", "1.0.1:SUCCEEDED:null");
        assertThat(attempts.getFirst().campaignId()).isEqualTo(campaign102);
        assertThat(attempts.getFirst().finishedAt()).isEqualTo(Instant.parse("2026-10-02T11:00:00Z"));
    }

    @Test
    @DisplayName("최대 개수만큼 최근 시도만 조회한다")
    void limitsToRecentAttempts() {
        insertEvent(vehicleKey, campaign101, "SUCCEEDED", null, "2026-10-01 10:00:00");
        insertEvent(vehicleKey, campaign102, "FAILED", "HASH_MISMATCH", "2026-10-02 10:00:00");
        insertEvent(vehicleKey, campaign102, "SUCCEEDED", null, "2026-10-02 11:00:00");

        List<VehicleUpdateAttempt> attempts = repository.findRecentAttempts(vehicleKey, Limit.of(2));

        assertThat(attempts).extracting(VehicleUpdateAttempt::result).containsExactly("SUCCEEDED", "FAILED");
    }

    @Test
    @DisplayName("다른 차량의 시도는 조회하지 않는다")
    void excludesOtherVehicles() {
        insertEvent(otherVehicleKey, campaign101, "SUCCEEDED", null, "2026-10-01 10:00:00");

        assertThat(repository.findRecentAttempts(vehicleKey, ALL)).isEmpty();
    }

    private Long persistVehicle(String externalId) {
        Vehicle vehicle = new Vehicle(new VehicleExternalId(externalId), new VehicleSpec("model-a", "hw-1", "KR"));
        em.persist(vehicle);
        return vehicle.getId();
    }

    private Long persistCampaign(String version) {
        ArtifactRelease release = new ArtifactRelease("model-a", version);
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        CampaignCondition condition = new CampaignCondition(
                new CampaignTarget("model-a", null, null),
                VersionRange.unbounded(),
                new CampaignPeriod(START_AT, START_AT.plus(Duration.ofDays(7))));
        Campaign campaign = new Campaign(new CampaignName("campaign-" + version), artifact.getId(), condition);
        em.persist(campaign);
        return campaign.getId();
    }

    // 세션 시간대는 UTC 로 고정돼 있다
    private void insertEvent(Long vehicle, Long campaign, String result, String failureReason, String finishedAt) {
        em.createNativeQuery("INSERT INTO vehicle_event"
                        + " (vehicle_id, campaign_id, result, failure_reason, finished_at)"
                        + " VALUES (:vehicleId, :campaignId, :result, :failureReason, :finishedAt)")
                .setParameter("vehicleId", vehicle)
                .setParameter("campaignId", campaign)
                .setParameter("result", result)
                .setParameter("failureReason", failureReason)
                .setParameter("finishedAt", finishedAt)
                .executeUpdate();
    }
}
