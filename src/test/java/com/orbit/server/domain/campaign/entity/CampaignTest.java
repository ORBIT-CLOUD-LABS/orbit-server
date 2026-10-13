package com.orbit.server.domain.campaign.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    @DisplayName("VEHICLE-POL-001: 차종·HW·지역·현재 버전 범위·적용 기간을 모두 만족하면 대상이다")
    void targetsVehicleMatchingAllConditions() {
        assertThat(campaign.targets(vehicleOf("hw-1", "1.0.5"), START)).isTrue();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 대상 HW 버전이 아니면 대상이 아니다")
    void notTargetWhenOutOfTarget() {
        assertThat(campaign.targets(vehicleOf("hw-3", "1.0.5"), START)).isFalse();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 현재 버전이 범위 밖이면 대상이 아니다")
    void notTargetWhenVersionOutOfRange() {
        assertThat(campaign.targets(vehicleOf("hw-1", "1.0.10"), START)).isFalse();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 적용 기간 밖이면 대상이 아니다")
    void notTargetWhenOutOfPeriod() {
        assertThat(campaign.targets(vehicleOf("hw-1", "1.0.5"), START.minusMillis(1)))
                .isFalse();
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 활성이고 적용 기간 안이면 배포 중이다")
    void activeWhenActiveStatusAndInPeriod() {
        assertThat(campaign.isActiveAt(START)).isTrue();
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 비활성이면 적용 기간 안이어도 배포 중이 아니다")
    void notActiveWhenInactiveStatus() {
        // 비활성화 API 가 아직 없으므로 테스트에서만 상태를 바꾼다
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.INACTIVE);

        assertThat(campaign.isActiveAt(START)).isFalse();
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 적용 기간 시작 전이면 배포 중이 아니다")
    void notActiveBeforeStart() {
        assertThat(campaign.isActiveAt(START.minusMillis(1))).isFalse();
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 적용 기간 종료 시각부터는 배포 중이 아니다")
    void notActiveFromEnd() {
        assertThat(campaign.isActiveAt(PERIOD.endAt())).isFalse();
    }

    private static TargetVehicle vehicleOf(String hwVersion, String currentVersion) {
        return new TargetVehicle("model-a", hwVersion, "KR", currentVersion);
    }

    private static CampaignCondition conditionOf(CampaignTarget target, VersionRange range) {
        return new CampaignCondition(target, range, PERIOD);
    }

    private static Campaign campaignOf(Long artifactId, CampaignCondition condition) {
        return new Campaign(new CampaignName("campaign"), artifactId, condition);
    }
}
