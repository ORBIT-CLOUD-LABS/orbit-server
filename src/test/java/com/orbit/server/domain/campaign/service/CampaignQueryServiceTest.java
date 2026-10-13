package com.orbit.server.domain.campaign.service;

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
import com.orbit.server.domain.campaign.entity.TargetVehicle;
import com.orbit.server.domain.campaign.entity.VersionRange;
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
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
class CampaignQueryServiceTest {

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant NOW = START.plus(Duration.ofHours(1));
    private static final CampaignPeriod PERIOD = new CampaignPeriod(START, START.plus(Duration.ofDays(1)));
    private static final TargetVehicle VEHICLE = new TargetVehicle("model-a", "hw-1", "KR", "1.0.0");

    @Autowired
    private CampaignQueryService service;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("VEHICLE-POL-001: 조건을 모두 만족하는 활성 캠페인을 대상으로 고른다")
    void findsMatchingCampaign() {
        Long campaignId = register("1.0.1", VersionRange.unbounded());

        assertThat(findTarget(VEHICLE)).contains(campaignId);
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 활성 캠페인이 없으면 대상이 아니다")
    void findsNothingWithoutActiveCampaign() {
        assertThat(findTarget(VEHICLE)).isEmpty();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 대상 조건이 맞지 않으면 대상이 아니다")
    void findsNothingWhenConditionMismatches() {
        register("1.0.1", VersionRange.unbounded());

        assertThat(findTarget(new TargetVehicle("model-a", "hw-9", "KR", "1.0.0")))
                .isEmpty();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 현재 버전이 이미 목표 버전이면 대상이 아니다")
    void findsNothingWhenAlreadyOnTargetVersion() {
        register("1.0.1", VersionRange.unbounded());

        assertThat(findTarget(new TargetVehicle("model-a", "hw-1", "KR", "1.0.1.0")))
                .isEmpty();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 여러 캠페인이 맞으면 목표 버전이 숫자로 가장 높은 캠페인을 고른다")
    void prefersHighestTargetVersion() {
        Long higher = register("1.0.10", VersionRange.unbounded());
        register("1.0.9", VersionRange.unbounded());

        assertThat(findTarget(VEHICLE)).contains(higher);
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 목표 버전이 같으면 나중에 등록한 캠페인을 고른다")
    void prefersLaterCampaignOnSameTargetVersion() {
        Long artifactId = saveArtifact("1.0.1");
        register(artifactId, VersionRange.unbounded());
        Long later = register(artifactId, new VersionRange("1.0.0", null));

        assertThat(findTarget(VEHICLE)).contains(later);
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 목표 버전을 숫자 자리로 읽을 수 없는 캠페인은 고르지 않는다")
    void skipsCampaignWithUnparsableTargetVersion() {
        register("1.0.1-beta", VersionRange.unbounded());

        assertThat(findTarget(VEHICLE)).isEmpty();
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 등록한 캠페인이면 있다고 확인한다")
    void confirmsRegisteredCampaign() {
        Long campaignId = register("1.0.1", VersionRange.unbounded());

        assertThat(service.exists(campaignId)).isTrue();
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 등록하지 않은 캠페인이면 없다고 확인한다")
    void confirmsMissingCampaign() {
        assertThat(service.exists(Long.MAX_VALUE)).isFalse();
    }

    // 지연 로딩하는 HW 버전·지역까지 DB 에서 다시 읽은 캠페인으로 판정하도록 영속성 컨텍스트를 비운다
    private Optional<Long> findTarget(TargetVehicle vehicle) {
        em.flush();
        em.clear();
        return service.findTargetCampaignId(vehicle, NOW);
    }

    private Long register(String targetVersion, VersionRange range) {
        return register(saveArtifact(targetVersion), range);
    }

    private Long register(Long artifactId, VersionRange range) {
        CampaignTarget target = new CampaignTarget("model-a", Set.of("hw-1"), Set.of("KR"));
        Campaign campaign =
                new Campaign(new CampaignName("campaign"), artifactId, new CampaignCondition(target, range, PERIOD));
        return campaignRepository.save(campaign).id();
    }

    private Long saveArtifact(String version) {
        ArtifactRelease release = new ArtifactRelease("model-a", version);
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        return artifact.id();
    }
}
