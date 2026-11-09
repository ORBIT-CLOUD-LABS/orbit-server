package com.orbit.server.domain.campaign.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TargetCampaignTest {

    // 목표 버전 1.0.10: 문자열 순서로는 1.0.9 보다 낮아 숫자 자리 비교를 확인할 수 있다
    private final TargetCampaign target =
            new TargetCampaign(1L, DottedVersion.parse("1.0.10").orElseThrow());

    @ParameterizedTest
    @ValueSource(strings = {"1.0.9", "1.0", "0.9.99"})
    @DisplayName("VEHICLE-POL-001: 현재 버전이 목표 버전보다 낮으면 올려야 한다")
    void upgradesLowerVersion(String currentVersion) {
        assertThat(target.upgrades(currentVersion)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0.10", "1.0.10.0", "1.0.11", "1.1", "2"})
    @DisplayName("VEHICLE-POL-001: 현재 버전이 목표 버전 이상이면 배포하지 않는다")
    void doesNotUpgradeSameOrHigherVersion(String currentVersion) {
        assertThat(target.upgrades(currentVersion)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1.0.9-rc1", "v1.0.9"})
    @DisplayName("VEHICLE-POL-001: 현재 버전을 숫자 자리로 읽을 수 없으면 비교할 수 없으므로 배포하지 않는다")
    void doesNotUpgradeUnparsableVersion(String currentVersion) {
        assertThat(target.upgrades(currentVersion)).isFalse();
    }
}
