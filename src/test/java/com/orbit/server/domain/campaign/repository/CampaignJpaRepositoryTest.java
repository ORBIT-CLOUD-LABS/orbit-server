package com.orbit.server.domain.campaign.repository;

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
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class CampaignJpaRepositoryTest {

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final CampaignPeriod PERIOD = new CampaignPeriod(START, START.plus(Duration.ofDays(1)));

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private EntityManager em;

    private Long artifactId;

    @BeforeEach
    void setUp() {
        ArtifactRelease release = new ArtifactRelease("model-a", "1.0.1");
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        artifactId = artifact.id();
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 차종으로 활성 캠페인만 조회한다")
    void findsActiveCampaignsByModel() {
        Campaign active = campaignRepository.save(campaignOf("model-a"));
        Campaign inactive = campaignRepository.save(campaignOf("model-a"));
        campaignRepository.save(campaignOf("model-b"));
        deactivate(inactive);

        List<Campaign> found = campaignRepository.findAllActiveByModel("model-a");

        assertThat(found).extracting(CampaignJpaRepositoryTest::idOf).containsExactly(idOf(active));
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: DB 에서 읽은 활성 캠페인과도 대상 조건을 비교해 중복을 판정한다")
    void judgesDuplicateAgainstReloadedCampaign() {
        campaignRepository.save(campaignOf("model-a"));
        em.flush();
        em.clear();

        Campaign reloaded = campaignRepository.findAllActiveByModel("model-a").getFirst();

        assertThat(campaignOf("model-a").duplicates(reloaded)).isTrue();
    }

    private Campaign campaignOf(String model) {
        CampaignTarget target = new CampaignTarget(model, Set.of("hw-1", "hw-2"), Set.of("KR"));
        CampaignCondition condition = new CampaignCondition(target, VersionRange.unbounded(), PERIOD);
        return new Campaign(new CampaignName("campaign"), artifactId, condition);
    }

    // 비활성화 기능은 아직 없으므로 DB 상태만 바꾼다
    private void deactivate(Campaign campaign) {
        em.flush();
        em.createNativeQuery("UPDATE campaign SET status = 'INACTIVE' WHERE id = :id")
                .setParameter("id", idOf(campaign))
                .executeUpdate();
        em.clear();
    }

    // 도메인 객체에 getter 를 두지 않으므로 테스트에서만 ID 를 꺼낸다
    private static Long idOf(Campaign campaign) {
        return (Long) ReflectionTestUtils.getField(campaign, "id");
    }
}
