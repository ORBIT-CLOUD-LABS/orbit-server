package com.orbit.server.domain.vehicle.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class VehicleTokenTest {

    // 32바이트를 패딩 없는 Base64URL 로 인코딩한 길이
    private static final int ISSUED_LENGTH = 43;

    @Test
    @DisplayName("VEHICLE-REQ-002: 헤더에 그대로 넣을 수 있는 43자 토큰을 발급한다")
    void issuesUrlSafeToken() {
        VehicleToken token = VehicleToken.issue();

        assertThat(token.value()).hasSize(ISSUED_LENGTH).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 발급할 때마다 다른 토큰을 만든다")
    void issuesDifferentTokens() {
        assertThat(VehicleToken.issue()).isNotEqualTo(VehicleToken.issue());
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 토큰 원문의 SHA-256 해시를 구한다")
    void hashesWithSha256() {
        // echo -n "abc" | shasum -a 256
        TokenHash expected = new TokenHash("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");

        assertThat(new VehicleToken("abc").hash()).isEqualTo(expected);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 문자열로 바꿔도 원문을 드러내지 않는다")
    void hidesValueInToString() {
        VehicleToken token = VehicleToken.issue();

        assertThat(token.toString()).doesNotContain(token.value());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("VEHICLE-POL-003: 비어 있는 토큰은 거부한다")
    void rejectsBlankToken(String value) {
        assertThatThrownBy(() -> new VehicleToken(value)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 128자를 넘는 토큰은 거부한다")
    void rejectsTooLongToken() {
        assertThatThrownBy(() -> new VehicleToken("a".repeat(129))).isInstanceOf(InvalidValueException.class);
    }
}
