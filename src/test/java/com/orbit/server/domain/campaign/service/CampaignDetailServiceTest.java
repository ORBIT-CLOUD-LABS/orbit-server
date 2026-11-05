package com.orbit.server.domain.campaign.service;

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
import com.orbit.server.domain.campaign.entity.CampaignStatus;
import com.orbit.server.domain.campaign.entity.CampaignTarget;
import com.orbit.server.domain.campaign.entity.VersionRange;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import com.orbit.server.global.error.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
class CampaignDetailServiceTest {

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant END = START.plus(Duration.ofDays(1));

    @Autowired
    private CampaignDetailService service;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private EntityManager em;

    // 보조 API(제안)라 요구사항 ID 가 없다
    @Test
    @DisplayName("캠페인 상세: 대상 조건·기간·상태와 배포하는 Artifact 의 버전을 목표 버전으로 조회한다")
    void getsDetail() {
        Long artifactId = saveArtifact("1.1.0");
        Long campaignId = saveAndClear(
                artifactId,
                new CampaignTarget("model-a", Set.of("hw-1"), Set.of("KR")),
                new VersionRange("1.0.0", "1.0.9"));

        CampaignDetail detail = service.getById(campaignId);

        assertThat(detail)
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new CampaignDetail(
                        campaignId,
                        "campaign",
                        artifactId,
                        "1.1.0",
                        new CampaignTargetDetail("model-a", List.of("hw-1"), List.of("KR"), "1.0.0", "1.0.9"),
                        CampaignStatus.ACTIVE,
                        START,
                        END,
                        null));
        assertThat(detail.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("캠페인 상세: HW 버전·지역은 이름 순서로 담는다")
    void sortsTargets() {
        Long campaignId = saveAndClear(
                saveArtifact("1.1.0"),
                new CampaignTarget("model-a", Set.of("hw-2", "hw-1"), Set.of("US", "KR")),
                VersionRange.unbounded());

        CampaignTargetDetail target = service.getById(campaignId).target();

        assertThat(target.hwVersions()).containsExactly("hw-1", "hw-2");
        assertThat(target.regions()).containsExactly("KR", "US");
    }

    @Test
    @DisplayName("캠페인 상세: HW 버전·지역이 전체 대상이면 빈 목록, 버전 범위 제한이 없으면 양 끝을 null 로 조회한다")
    void emptyTargetsAndUnboundedRange() {
        Long campaignId = saveAndClear(
                saveArtifact("1.1.0"), new CampaignTarget("model-a", Set.of(), Set.of()), VersionRange.unbounded());

        CampaignTargetDetail target = service.getById(campaignId).target();

        assertThat(target.hwVersions()).isEmpty();
        assertThat(target.regions()).isEmpty();
        assertThat(target.currentVersionMin()).isNull();
        assertThat(target.currentVersionMax()).isNull();
    }

    @Test
    @DisplayName("캠페인 상세: 캠페인이 없으면 CAMPAIGN_NOT_FOUND")
    void rejectsNotFound() {
        assertThatThrownBy(() -> service.getById(Long.MAX_VALUE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CampaignErrorCode.CAMPAIGN_NOT_FOUND);
    }

    private Long saveArtifact(String version) {
        ArtifactRelease release = new ArtifactRelease("model-a", version);
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        return artifact.getId();
    }

    // 영속성 컨텍스트에 남은 엔티티가 아니라 DB 에서 다시 읽은 값으로 변환하는지 확인하도록 비운다
    private Long saveAndClear(Long artifactId, CampaignTarget target, VersionRange currentVersionRange) {
        CampaignCondition condition =
                new CampaignCondition(target, currentVersionRange, new CampaignPeriod(START, END));
        Campaign campaign = campaignRepository.save(new Campaign(new CampaignName("campaign"), artifactId, condition));
        em.flush();
        em.clear();
        return campaign.getId();
    }
}
