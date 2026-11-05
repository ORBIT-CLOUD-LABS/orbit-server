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
class CampaignDeactivationServiceTest {

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant END = START.plus(Duration.ofDays(1));

    @Autowired
    private CampaignDeactivationService service;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private EntityManager em;

    // 보조 API(제안)라 요구사항 ID 가 없다
    @Test
    @DisplayName("캠페인 비활성화: 활성 캠페인을 비활성으로 저장하고 상세를 돌려준다")
    void deactivatesCampaign() {
        Long campaignId = saveAndClear(saveArtifact());

        CampaignDetail detail = service.deactivate(campaignId);

        assertThat(detail.campaignId()).isEqualTo(campaignId);
        assertThat(detail.status()).isEqualTo(CampaignStatus.INACTIVE);
        assertThat(detail.targetVersion()).isEqualTo("1.1.0");
        assertThat(reloadStatus(campaignId)).isEqualTo(CampaignStatus.INACTIVE);
    }

    @Test
    @DisplayName("캠페인 비활성화: 이미 비활성이면 그대로 두고 상세를 돌려준다")
    void keepsInactiveWhenAlreadyInactive() {
        Long campaignId = saveAndClear(saveArtifact());
        service.deactivate(campaignId);

        CampaignDetail detail = service.deactivate(campaignId);

        assertThat(detail.status()).isEqualTo(CampaignStatus.INACTIVE);
        assertThat(reloadStatus(campaignId)).isEqualTo(CampaignStatus.INACTIVE);
    }

    @Test
    @DisplayName("캠페인 비활성화: 캠페인이 없으면 CAMPAIGN_NOT_FOUND")
    void rejectsNotFound() {
        assertThatThrownBy(() -> service.deactivate(Long.MAX_VALUE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CampaignErrorCode.CAMPAIGN_NOT_FOUND);
    }

    private Long saveArtifact() {
        ArtifactRelease release = new ArtifactRelease("model-a", "1.1.0");
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        return artifact.getId();
    }

    private Long saveAndClear(Long artifactId) {
        CampaignCondition condition = new CampaignCondition(
                new CampaignTarget("model-a", Set.of(), Set.of()),
                VersionRange.unbounded(),
                new CampaignPeriod(START, END));
        Campaign campaign = campaignRepository.save(new Campaign(new CampaignName("campaign"), artifactId, condition));
        em.flush();
        em.clear();
        return campaign.getId();
    }

    // 변경 감지로 UPDATE 가 나갔는지 보도록 영속성 컨텍스트를 비우고 DB 에서 다시 읽는다
    private CampaignStatus reloadStatus(Long campaignId) {
        em.flush();
        em.clear();
        return campaignRepository.findById(campaignId).orElseThrow().getStatus();
    }
}
