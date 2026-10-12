package com.orbit.server.domain.campaign.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DottedVersionTest {

    @ParameterizedTest
    @CsvSource({"1.0.10, 1.0.9", "2.0, 1.9.9", "1.0.1, 1.0", "10, 9"})
    @DisplayName("VEHICLE-POL-001: 문자열 순서가 아니라 자리별 숫자로 비교한다")
    void comparesSegmentsAsNumbers(String higher, String lower) {
        assertThat(parse(higher)).isGreaterThan(parse(lower));
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 빈 자리는 0 으로 보아 1.0 과 1.0.0 은 같은 버전이다")
    void treatsMissingSegmentsAsZero() {
        assertThat(parse("1.0")).isEqualTo(parse("1.0.0")).isEqualByComparingTo(parse("1.0.0"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1.0-beta", "v1.0", "1..0", "1.0.", ".1", "-1.0", "1.9999999999"})
    @DisplayName("VEHICLE-POL-001: 숫자 자리로 읽을 수 없는 버전은 빈 값이다")
    void parsesNothingFromNonNumericVersion(String value) {
        assertThat(DottedVersion.parse(value)).isEmpty();
    }

    private DottedVersion parse(String value) {
        return DottedVersion.parse(value).orElseThrow();
    }
}
