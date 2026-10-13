package com.orbit.server.domain.campaign.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VersionRangeTest {

    private final VersionRange range = new VersionRange("1.0.9", "1.0.10");

    @ParameterizedTest
    @ValueSource(strings = {"1.0.9", "1.0.10", "1.0.9.1"})
    @DisplayName("VEHICLE-POL-001: 양 끝을 포함해 숫자 자리로 범위를 판정한다")
    void containsVersionWithinRange(String currentVersion) {
        assertThat(range.contains(currentVersion)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0.8", "1.0.11", "1.1"})
    @DisplayName("VEHICLE-POL-001: 범위 밖의 버전은 대상이 아니다")
    void excludesVersionOutOfRange(String currentVersion) {
        assertThat(range.contains(currentVersion)).isFalse();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 생략한 쪽은 제한하지 않는다")
    void ignoresOmittedBound() {
        assertThat(new VersionRange(null, "2.0").contains("0.1")).isTrue();
        assertThat(new VersionRange("1.0", null).contains("99.0")).isTrue();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 제한이 없는 범위는 버전 형식과 상관없이 대상이다")
    void containsAnyVersionWhenUnbounded() {
        assertThat(VersionRange.unbounded().contains("1.0-beta")).isTrue();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 비교할 버전을 숫자 자리로 읽을 수 없으면 대상이 아니다")
    void excludesUnparsableVersion() {
        assertThat(range.contains("1.0-beta")).isFalse();
        assertThat(new VersionRange("v1.0", null).contains("1.0")).isFalse();
    }
}
