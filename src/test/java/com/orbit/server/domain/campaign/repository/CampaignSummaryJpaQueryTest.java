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
import com.orbit.server.domain.campaign.entity.CampaignStatus;
import com.orbit.server.domain.campaign.entity.CampaignTarget;
import com.orbit.server.domain.campaign.entity.VersionRange;
import com.orbit.server.domain.campaign.service.CampaignFilter;
import com.orbit.server.domain.campaign.service.CampaignSummary;
import com.orbit.server.domain.campaign.service.CampaignSummaryQuery;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class CampaignSummaryJpaQueryTest {

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant END = START.plus(Duration.ofDays(1));

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignSummaryQuery campaignSummaryQuery;

    @Autowired
    private EntityManager em;

    private Long artifactId;

    private Campaign activeA;
    private Campaign inactiveA;
    private Campaign activeB;
    private Campaign inactiveB;

    @BeforeEach
    void setUp() {
        ArtifactRelease release = new ArtifactRelease("model-a", "1.0.1");
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        artifactId = artifact.id();

        activeA = campaignRepository.save(campaignOf("model-a"));
        inactiveA = campaignRepository.save(campaignOf("model-a"));
        activeB = campaignRepository.save(campaignOf("model-b"));
        inactiveB = campaignRepository.save(campaignOf("model-b"));
        deactivate(inactiveA);
        deactivate(inactiveB);
    }

    @Test
    @DisplayName("#18: 필터가 없으면 전체 캠페인을 최근 등록한 순서로 조회한다")
    void findAllWithoutFilter() {
        List<CampaignSummary> summaries = campaignSummaryQuery.findAll(new CampaignFilter(null, null));

        assertThat(summaries)
                .extracting(CampaignSummary::campaignId)
                .containsExactly(inactiveB.id(), activeB.id(), inactiveA.id(), activeA.id());
    }

    @Test
    @DisplayName("#18: 상태로만 필터링하면 그 상태의 캠페인만 조회한다")
    void findAllByStatus() {
        List<CampaignSummary> summaries =
                campaignSummaryQuery.findAll(new CampaignFilter(CampaignStatus.INACTIVE, null));

        assertThat(summaries).extracting(CampaignSummary::campaignId).containsExactly(inactiveB.id(), inactiveA.id());
    }

    @Test
    @DisplayName("#18: 차종으로만 필터링하면 그 차종의 캠페인만 조회한다")
    void findAllByModel() {
        List<CampaignSummary> summaries = campaignSummaryQuery.findAll(new CampaignFilter(null, "model-a"));

        assertThat(summaries).extracting(CampaignSummary::campaignId).containsExactly(inactiveA.id(), activeA.id());
    }

    @Test
    @DisplayName("#18: 상태와 차종으로 함께 필터링하면 둘 다 맞는 캠페인만 조회한다")
    void findAllByStatusAndModel() {
        List<CampaignSummary> summaries =
                campaignSummaryQuery.findAll(new CampaignFilter(CampaignStatus.ACTIVE, "model-b"));

        assertThat(summaries).extracting(CampaignSummary::campaignId).containsExactly(activeB.id());
    }

    @Test
    @DisplayName("#18: 필터에 맞는 캠페인이 없으면 빈 목록을 조회한다")
    void emptyWhenNoMatch() {
        assertThat(campaignSummaryQuery.findAll(new CampaignFilter(null, "model-c")))
                .isEmpty();
    }

    @Test
    @DisplayName("#18: 조회 결과에 캠페인 정보와 등록 시각을 담는다")
    void summaryContainsCampaignInfo() {
        CampaignSummary summary = campaignSummaryQuery
                .findAll(new CampaignFilter(CampaignStatus.ACTIVE, "model-a"))
                .getFirst();

        assertThat(summary)
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new CampaignSummary(
                        activeA.id(), artifactId, "campaign", "model-a", CampaignStatus.ACTIVE, START, END, null));
        assertThat(summary.createdAt()).isNotNull();
    }

    private Campaign campaignOf(String model) {
        CampaignTarget target = new CampaignTarget(model, Set.of("hw-1"), Set.of("KR"));
        CampaignCondition condition =
                new CampaignCondition(target, VersionRange.unbounded(), new CampaignPeriod(START, END));
        return new Campaign(new CampaignName("campaign"), artifactId, condition);
    }

    // 비활성화 기능은 아직 없으므로 DB 상태만 바꾼다
    private void deactivate(Campaign campaign) {
        em.flush();
        em.createNativeQuery("UPDATE campaign SET status = 'INACTIVE' WHERE id = :id")
                .setParameter("id", campaign.id())
                .executeUpdate();
        em.clear();
    }
}
