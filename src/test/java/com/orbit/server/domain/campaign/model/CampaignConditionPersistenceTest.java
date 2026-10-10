package com.orbit.server.domain.campaign.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.artifact.model.Artifact;
import com.orbit.server.domain.artifact.model.ArtifactFile;
import com.orbit.server.domain.artifact.model.ArtifactRelease;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
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

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final CampaignPeriod PERIOD = new CampaignPeriod(START, START.plus(Duration.ofDays(1)));

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

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 적용 기간은 JVM 시간대와 무관하게 UTC 로 저장하고 같은 시각으로 복원한다")
    void storesPeriodInUtc() {
        Long campaignId = persistCampaign(VersionRange.unbounded());
        em.clear();

        Object storedStartAt = em.createNativeQuery("SELECT CAST(start_at AS CHAR) FROM campaign WHERE id = :id")
                .setParameter("id", campaignId)
                .getSingleResult();
        Campaign found = em.find(Campaign.class, campaignId);

        assertThat(storedStartAt).isEqualTo("2026-10-01 00:00:00.000");
        CampaignCondition condition = (CampaignCondition) ReflectionTestUtils.getField(found, "condition");
        assertThat(ReflectionTestUtils.getField(condition, "period")).isEqualTo(PERIOD);
    }

    private Long persistCampaign(VersionRange versionRange) {
        Artifact artifact = new Artifact(
                new ArtifactRelease("model-a", "1.0.0"),
                new ArtifactFile("a".repeat(64), 1, "/firmware/model-a/1.0.0/firmware.bin"));
        em.persist(artifact);
        CampaignCondition condition =
                new CampaignCondition(new CampaignTarget("model-a", null, null), versionRange, PERIOD);
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
