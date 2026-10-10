package com.orbit.server.domain.campaign.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.artifact.model.Artifact;
import com.orbit.server.domain.artifact.model.ArtifactFile;
import com.orbit.server.domain.artifact.model.ArtifactRelease;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignConditionPersistenceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 1, 0, 0);

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 버전 범위 제한이 없는 캠페인을 다시 조회해도 제한 없는 범위로 복원한다")
    void restoresUnboundedVersionRangeAfterReload() {
        Long campaignId = persistCampaign(VersionRange.unbounded());
        em.clear();

        Campaign found = em.find(Campaign.class, campaignId);

        CampaignCondition condition = (CampaignCondition) ReflectionTestUtils.getField(found, "condition");
        assertThat(condition.currentVersionRange()).isEqualTo(VersionRange.unbounded());
    }

    private Long persistCampaign(VersionRange versionRange) {
        Artifact artifact = new Artifact(
                new ArtifactRelease("model-a", "1.0.0"),
                new ArtifactFile("a".repeat(64), 1, "/firmware/model-a/1.0.0/firmware.bin"));
        em.persist(artifact);
        CampaignCondition condition = new CampaignCondition(
                new CampaignTarget("model-a", null, null), versionRange, new CampaignPeriod(START, START.plusDays(1)));
        Campaign campaign = new Campaign(new CampaignName("campaign"), idOf(artifact), condition);
        em.persist(campaign);
        em.flush();
        return idOf(campaign);
    }

    // 도메인 객체에 getter 를 두지 않으므로 테스트에서만 ID 를 꺼낸다
    private static Long idOf(Object entity) {
        return (Long) ReflectionTestUtils.getField(entity, "id");
    }
}
