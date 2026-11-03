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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 엔티티가 아직 매핑하지 않은 [Phase2] 다운로드 Edge 컬럼과 실패 사유 제약을 SQL 로 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class UpdateResultSchemaTest {

    private static final Instant START_AT = Instant.parse("2026-10-01T12:00:00Z");

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
                new CampaignPeriod(START_AT, START_AT.plus(Duration.ofDays(1))));
        Campaign campaign = new Campaign(new CampaignName("campaign"), artifact.id(), condition);
        em.persist(campaign);
        campaignId = campaign.id();
    }

    @Test
    @DisplayName("다운로드 Edge 없이 결과를 기록할 수 있다")
    void allowsResultWithoutEdge() {
        insert("SUCCEEDED", null, null);

        assertThat(edgeId()).isEqualTo("null");
    }

    @Test
    @DisplayName("실제로 파일을 받은 Edge 를 기록한다")
    void storesEdge() {
        insert("FAILED", "HASH_MISMATCH", "edge-daejeon");

        assertThat(edgeId()).isEqualTo("edge-daejeon");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"DOWNLOAD_FAILED", "HASH_MISMATCH", "INSTALL_FAILED", "SIGNATURE_INVALID", "DOWNGRADE_REJECTED"})
    @DisplayName("기존 실패 사유와 [Phase2] 서명·다운그레이드 실패 사유를 허용한다")
    void allowsFailureReasons(String failureReason) {
        insert("FAILED", failureReason, null);

        assertThat(edgeId()).isEqualTo("null");
    }

    @Test
    @DisplayName("정의하지 않은 실패 사유는 저장하지 않는다")
    void rejectsUnknownFailureReason() {
        assertThatThrownBy(() -> insert("FAILED", "UNKNOWN", null)).isInstanceOf(PersistenceException.class);
    }

    private void insert(String result, String failureReason, String edgeId) {
        em.createNativeQuery("INSERT INTO update_result"
                        + " (vehicle_id, campaign_id, result, failure_reason, finished_at, edge_id)"
                        + " VALUES (:vehicleId, :campaignId, :result, :failureReason, '2026-10-01 12:00:00', :edgeId)")
                .setParameter("vehicleId", vehicleId)
                .setParameter("campaignId", campaignId)
                .setParameter("result", result)
                .setParameter("failureReason", failureReason)
                .setParameter("edgeId", edgeId)
                .executeUpdate();
    }

    private String edgeId() {
        Object edgeId = em.createNativeQuery("SELECT edge_id FROM update_result WHERE vehicle_id = :id")
                .setParameter("id", vehicleId)
                .getSingleResult();
        return String.valueOf(edgeId);
    }
}
