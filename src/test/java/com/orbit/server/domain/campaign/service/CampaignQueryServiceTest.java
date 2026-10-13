package com.orbit.server.domain.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactFile;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.artifact.service.ArtifactSummary;
import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.entity.CampaignCondition;
import com.orbit.server.domain.campaign.entity.CampaignName;
import com.orbit.server.domain.campaign.entity.CampaignPeriod;
import com.orbit.server.domain.campaign.entity.CampaignTarget;
import com.orbit.server.domain.campaign.entity.TargetVehicle;
import com.orbit.server.domain.campaign.entity.VersionRange;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import com.orbit.server.global.error.BusinessException;
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

    @Test
    @DisplayName("MANIFEST-POL-001: 요청한 캠페인의 대상이면 배포할 파일을 돌려준다")
    void getsArtifactOfTargetCampaign() {
        Long campaignId = register("1.0.1", VersionRange.unbounded());

        ArtifactSummary artifact = getTargetArtifact(campaignId, VEHICLE, NOW);

        assertThat(artifact.version()).isEqualTo("1.0.1");
        assertThat(artifact.path()).isEqualTo("/firmware/model-a/1.0.1/firmware.bin");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 더 높은 버전의 캠페인이 있어도 요청한 캠페인의 파일을 돌려준다")
    void getsRequestedCampaignEvenIfHigherExists() {
        Long requested = register("1.0.1", VersionRange.unbounded());
        register("1.0.2", VersionRange.unbounded());

        assertThat(getTargetArtifact(requested, VEHICLE, NOW).version()).isEqualTo("1.0.1");
    }

    @Test
    @DisplayName("MANIFEST-REQ-001: 캠페인이 없으면 CAMPAIGN_NOT_FOUND")
    void rejectsMissingCampaign() {
        assertRejected(Long.MAX_VALUE, VEHICLE, NOW, CampaignErrorCode.CAMPAIGN_NOT_FOUND);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 비활성 캠페인이면 CAMPAIGN_INACTIVE")
    void rejectsInactiveCampaign() {
        Long campaignId = register("1.0.1", VersionRange.unbounded());
        deactivate(campaignId);

        assertRejected(campaignId, VEHICLE, NOW, CampaignErrorCode.CAMPAIGN_INACTIVE);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 적용 기간 시작 전이면 CAMPAIGN_INACTIVE")
    void rejectsBeforeStart() {
        Long campaignId = register("1.0.1", VersionRange.unbounded());

        assertRejected(campaignId, VEHICLE, START.minusMillis(1), CampaignErrorCode.CAMPAIGN_INACTIVE);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 적용 기간이 끝났으면 CAMPAIGN_INACTIVE")
    void rejectsAfterEnd() {
        Long campaignId = register("1.0.1", VersionRange.unbounded());

        assertRejected(campaignId, VEHICLE, PERIOD.endAt(), CampaignErrorCode.CAMPAIGN_INACTIVE);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 비활성이면서 대상도 아니면 CAMPAIGN_INACTIVE 를 먼저 응답한다")
    void rejectsInactiveBeforeNotTarget() {
        Long campaignId = register("1.0.1", VersionRange.unbounded());
        deactivate(campaignId);

        assertRejected(
                campaignId,
                new TargetVehicle("model-a", "hw-9", "KR", "1.0.0"),
                NOW,
                CampaignErrorCode.CAMPAIGN_INACTIVE);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 대상 조건이 맞지 않으면 NOT_UPDATE_TARGET")
    void rejectsWhenConditionMismatches() {
        Long campaignId = register("1.0.1", VersionRange.unbounded());

        assertRejected(
                campaignId,
                new TargetVehicle("model-a", "hw-9", "KR", "1.0.0"),
                NOW,
                CampaignErrorCode.NOT_UPDATE_TARGET);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 현재 버전이 이미 목표 버전이면 NOT_UPDATE_TARGET")
    void rejectsWhenAlreadyOnTargetVersion() {
        Long campaignId = register("1.0.1", VersionRange.unbounded());

        assertRejected(
                campaignId,
                new TargetVehicle("model-a", "hw-1", "KR", "1.0.1"),
                NOW,
                CampaignErrorCode.NOT_UPDATE_TARGET);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 체크인처럼 목표 버전을 숫자 자리로 읽을 수 없으면 NOT_UPDATE_TARGET")
    void rejectsUnparsableTargetVersion() {
        Long campaignId = register("1.0.1-beta", VersionRange.unbounded());

        assertRejected(campaignId, VEHICLE, NOW, CampaignErrorCode.NOT_UPDATE_TARGET);
    }

    // 지연 로딩하는 HW 버전·지역까지 DB 에서 다시 읽은 캠페인으로 판정하도록 영속성 컨텍스트를 비운다
    private Optional<Long> findTarget(TargetVehicle vehicle) {
        em.flush();
        em.clear();
        return service.findTargetCampaignId(vehicle, NOW);
    }

    private ArtifactSummary getTargetArtifact(Long campaignId, TargetVehicle vehicle, Instant now) {
        em.flush();
        em.clear();
        return service.getTargetArtifact(campaignId, vehicle, now);
    }

    private void assertRejected(Long campaignId, TargetVehicle vehicle, Instant now, CampaignErrorCode errorCode) {
        assertThatThrownBy(() -> getTargetArtifact(campaignId, vehicle, now))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    // 비활성화 기능은 아직 없으므로 DB 상태만 바꾼다
    private void deactivate(Long campaignId) {
        em.flush();
        em.createNativeQuery("UPDATE campaign SET status = 'INACTIVE' WHERE id = :id")
                .setParameter("id", campaignId)
                .executeUpdate();
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
