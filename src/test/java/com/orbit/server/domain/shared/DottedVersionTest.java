package com.orbit.server.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
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
    @CsvSource({"1, 1", "1.0.1, 1.0.1", "10.20.300, 10.20.300", "1.0.0, 1"})
    @DisplayName("VEHICLE-POL-001: 점으로 나눈 숫자 형식의 입력은 버전으로 읽는다")
    void createsFromDottedNumbers(String value, String expected) {
        assertThat(parse(value)).hasToString(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1.0-beta", "v1.0", "1.0.1-rc1", "1..0", "1.0.", ".1", "-1.0", " 1.0", "1.9999999999"})
    @DisplayName("VEHICLE-POL-001: 점으로 나눈 숫자 형식이 아닌 입력은 거부한다")
    void rejectsNonDottedInput(String value) {
        assertThatThrownBy(() -> DottedVersion.of(value, "버전"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("버전");
    }

    private DottedVersion parse(String value) {
        return DottedVersion.of(value, "버전");
    }
}
