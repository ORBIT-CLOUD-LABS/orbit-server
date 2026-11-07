package com.orbit.server.domain.update.repository;

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
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 업데이트 시도 이력(vehicle_event)의 중복·실패 정보 제약을 SQL 로 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class VehicleEventSchemaTest {

    private static final Instant START_AT = Instant.parse("2026-10-01T12:00:00Z");
    private static final String FIRST_FINISHED_AT = "2026-10-01 12:00:00";
    private static final String RETRY_FINISHED_AT = "2026-10-01 12:10:00";

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
                new CampaignPeriod(START_AT, START_AT.plus(Duration.ofDays(1))));
        Campaign campaign = new Campaign(new CampaignName("campaign"), artifact.getId(), condition);
        em.persist(campaign);
        campaignId = campaign.getId();
    }

    @Test
    @DisplayName("같은 캠페인의 실패 후 재시도는 완료 시각이 달라 따로 남는다")
    void keepsRetryAsSeparateAttempt() {
        insert("FAILED", "HASH_MISMATCH", FIRST_FINISHED_AT);
        insert("SUCCEEDED", null, RETRY_FINISHED_AT);

        assertThat(countEvents()).isEqualTo(2);
    }

    @Test
    @DisplayName("완료 시각까지 같은 결과는 같은 시도의 재전송이라 저장하지 않는다")
    void rejectsSameAttempt() {
        insert("FAILED", "HASH_MISMATCH", FIRST_FINISHED_AT);

        assertThatThrownBy(() -> insert("FAILED", "HASH_MISMATCH", FIRST_FINISHED_AT))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("실패 사유 없는 실패는 저장하지 않는다")
    void rejectsFailureWithoutReason() {
        assertThatThrownBy(() -> insert("FAILED", null, FIRST_FINISHED_AT)).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("실패 사유가 있는 성공은 저장하지 않는다")
    void rejectsSuccessWithReason() {
        assertThatThrownBy(() -> insert("SUCCEEDED", "HASH_MISMATCH", FIRST_FINISHED_AT))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("정의하지 않은 실패 사유는 저장하지 않는다")
    void rejectsUnknownFailureReason() {
        assertThatThrownBy(() -> insert("FAILED", "UNKNOWN", FIRST_FINISHED_AT))
                .isInstanceOf(PersistenceException.class);
    }

    private void insert(String result, String failureReason, String finishedAt) {
        em.createNativeQuery("INSERT INTO vehicle_event"
                        + " (vehicle_id, campaign_id, result, failure_reason, finished_at)"
                        + " VALUES (:vehicleId, :campaignId, :result, :failureReason, :finishedAt)")
                .setParameter("vehicleId", vehicleId)
                .setParameter("campaignId", campaignId)
                .setParameter("result", result)
                .setParameter("failureReason", failureReason)
                .setParameter("finishedAt", finishedAt)
                .executeUpdate();
    }

    private long countEvents() {
        Object count = em.createNativeQuery("SELECT COUNT(*) FROM vehicle_event WHERE vehicle_id = :id")
                .setParameter("id", vehicleId)
                .getSingleResult();
        return ((Number) count).longValue();
    }
}
