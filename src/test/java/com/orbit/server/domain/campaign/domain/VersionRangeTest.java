package com.orbit.server.domain.campaign.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
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
    @DisplayName("VEHICLE-POL-001: 제한이 없는 범위는 모든 버전이 대상이다")
    void containsAnyVersionWhenUnbounded() {
        assertThat(VersionRange.unbounded().contains("0.0.1")).isTrue();
        assertThat(VersionRange.unbounded().contains("99.0")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("VEHICLE-POL-001: 비워 둔 쪽은 생략한 것으로 보아 제한하지 않는다")
    void treatsBlankBoundAsOmitted(String bound) {
        assertThat(new VersionRange(bound, bound)).isEqualTo(VersionRange.unbounded());
        assertThat(new VersionRange(bound, "2.0").contains("0.1")).isTrue();
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 양 끝의 표기가 달라도 비교 규칙상 같으면 같은 범위다")
    void sameRangeWhenBoundsAreEquivalent() {
        assertThat(new VersionRange("1.0", "2").isSameRangeAs(new VersionRange("1.0.0", "02.0")))
                .isTrue();
        assertThat(new VersionRange("1.0", null).isSameRangeAs(new VersionRange("1.0.0", null)))
                .isTrue();
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 비교 규칙상 한 끝이라도 다르면 다른 범위다")
    void differentRangeWhenBoundDiffers() {
        assertThat(range.isSameRangeAs(new VersionRange("1.0.9", "1.0.11"))).isFalse();
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 제한이 없는 쪽은 제한이 없는 쪽과만 같다")
    void omittedBoundEqualsOnlyOmittedBound() {
        assertThat(new VersionRange(null, "2.0").isSameRangeAs(new VersionRange("0", "2.0")))
                .isFalse();
        assertThat(new VersionRange("1.0", null).isSameRangeAs(new VersionRange("1.0", "99.0")))
                .isFalse();
        assertThat(VersionRange.unbounded().isSameRangeAs(VersionRange.unbounded()))
                .isTrue();
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 최소 버전이 최대 버전보다 크면 대상이 될 차량이 없으므로 거부한다")
    void rejectsMinGreaterThanMax() {
        assertThatThrownBy(() -> new VersionRange("2.0", "1.0")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new VersionRange("1.0.10", "1.0.9")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 최소 버전과 최대 버전이 비교 규칙상 같으면 그 버전만 대상인 범위로 받는다")
    void acceptsMinEqualToMax() {
        VersionRange single = new VersionRange("1.0.0", "1.0");

        assertThat(single.contains("1.0")).isTrue();
        assertThat(single.contains("1.0.1")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1.0", "1.0.0-rc1", "1..0"})
    @DisplayName("VEHICLE-POL-001: 범위의 버전이 점으로 나눈 숫자 형식이 아니면 비교할 수 없어 거부한다")
    void rejectsNonDottedBound(String bound) {
        assertThatThrownBy(() -> new VersionRange(bound, null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new VersionRange(null, bound)).isInstanceOf(InvalidValueException.class);
    }
}
