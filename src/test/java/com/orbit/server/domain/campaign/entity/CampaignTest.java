package com.orbit.server.domain.campaign.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CampaignTest {

    private static final Long ARTIFACT_ID = 1L;
    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final CampaignPeriod PERIOD = new CampaignPeriod(START, START.plus(Duration.ofDays(1)));
    private static final CampaignTarget TARGET = new CampaignTarget("model-a", Set.of("hw-1", "hw-2"), Set.of("KR"));
    private static final VersionRange RANGE = new VersionRange("1.0.0", "1.0.9");

    private final Campaign campaign = campaignOf(ARTIFACT_ID, conditionOf(TARGET, RANGE));

    @Test
    @DisplayName("CAMPAIGN-POL-001: 파일과 대상 조건이 모두 같으면 중복이다")
    void duplicatesWhenSameArtifactAndTarget() {
        CampaignTarget sameTarget = new CampaignTarget("model-a", Set.of("hw-2", "hw-1"), Set.of("KR"));
        Campaign other = campaignOf(ARTIFACT_ID, conditionOf(sameTarget, new VersionRange("1.0.0", "1.0.9")));

        assertThat(campaign.duplicates(other)).isTrue();
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 적용 기간이 달라도 파일과 대상 조건이 같으면 중복이다")
    void duplicatesRegardlessOfPeriod() {
        CampaignPeriod later = new CampaignPeriod(START.plus(Duration.ofDays(7)), START.plus(Duration.ofDays(8)));
        Campaign other = campaignOf(ARTIFACT_ID, new CampaignCondition(TARGET, RANGE, later));

        assertThat(campaign.duplicates(other)).isTrue();
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 파일이 다르면 중복이 아니다")
    void notDuplicateWhenDifferentArtifact() {
        Campaign other = campaignOf(2L, conditionOf(TARGET, RANGE));

        assertThat(campaign.duplicates(other)).isFalse();
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: HW 버전이 일부만 겹치면 중복이 아니다")
    void notDuplicateWhenHwVersionsPartiallyOverlap() {
        CampaignTarget target = new CampaignTarget("model-a", Set.of("hw-1"), Set.of("KR"));
        Campaign other = campaignOf(ARTIFACT_ID, conditionOf(target, RANGE));

        assertThat(campaign.duplicates(other)).isFalse();
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 지역이 다르면 중복이 아니다")
    void notDuplicateWhenDifferentRegions() {
        CampaignTarget target = new CampaignTarget("model-a", Set.of("hw-1", "hw-2"), Set.of());
        Campaign other = campaignOf(ARTIFACT_ID, conditionOf(target, RANGE));

        assertThat(campaign.duplicates(other)).isFalse();
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 현재 버전 범위가 다르면 중복이 아니다")
    void notDuplicateWhenDifferentVersionRange() {
        Campaign other = campaignOf(ARTIFACT_ID, conditionOf(TARGET, VersionRange.unbounded()));

        assertThat(campaign.duplicates(other)).isFalse();
    }

    private static CampaignCondition conditionOf(CampaignTarget target, VersionRange range) {
        return new CampaignCondition(target, range, PERIOD);
    }

    private static Campaign campaignOf(Long artifactId, CampaignCondition condition) {
        return new Campaign(new CampaignName("campaign"), artifactId, condition);
    }
}
