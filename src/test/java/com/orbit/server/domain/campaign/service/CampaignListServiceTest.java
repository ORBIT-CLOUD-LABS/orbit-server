package com.orbit.server.domain.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
import com.orbit.server.domain.campaign.repository.CampaignRepository;
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
class CampaignListServiceTest {

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final CampaignPeriod PERIOD = new CampaignPeriod(START, START.plus(Duration.ofDays(1)));

    @Autowired
    private CampaignListService service;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("#18: 캠페인마다 배포하는 Artifact 의 버전을 목표 버전으로 붙인다")
    void attachesTargetVersion() {
        Long older = register("model-a", "1.0.1");
        Long newer = register("model-a", "1.0.2");

        assertThat(service.findAll(new CampaignFilter(null, null)))
                .extracting(item -> item.campaign().campaignId(), CampaignListItem::targetVersion)
                .containsExactly(tuple(newer, "1.0.2"), tuple(older, "1.0.1"));
    }

    @Test
    @DisplayName("#18: 필터에 맞는 캠페인만 목표 버전과 함께 조회한다")
    void appliesFilter() {
        register("model-a", "1.0.1");
        Long modelB = register("model-b", "2.0.0");

        assertThat(service.findAll(new CampaignFilter(CampaignStatus.ACTIVE, "model-b")))
                .extracting(item -> item.campaign().campaignId(), CampaignListItem::targetVersion)
                .containsExactly(tuple(modelB, "2.0.0"));
    }

    @Test
    @DisplayName("#18: 필터에 맞는 캠페인이 없으면 빈 목록을 조회한다")
    void emptyWhenNoCampaign() {
        register("model-a", "1.0.1");

        assertThat(service.findAll(new CampaignFilter(null, "model-c"))).isEmpty();
    }

    private Long register(String model, String version) {
        ArtifactRelease release = new ArtifactRelease(model, version);
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        CampaignCondition condition =
                new CampaignCondition(new CampaignTarget(model, Set.of(), Set.of()), VersionRange.unbounded(), PERIOD);
        return campaignRepository
                .save(new Campaign(new CampaignName("campaign"), artifact.id(), condition))
                .id();
    }
}
