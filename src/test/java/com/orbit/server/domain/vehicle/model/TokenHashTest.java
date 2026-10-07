package com.orbit.server.domain.vehicle.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TokenHashTest {

    @Test
    @DisplayName("VEHICLE-REQ-002: 소문자 16진수 64자 해시로 생성한다")
    void createsWithValidHash() {
        assertThatCode(() -> new TokenHash("0123456789abcdef".repeat(4))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "raw-token", "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"})
    @DisplayName("VEHICLE-REQ-002: 해시 형식이 아니면 거부한다")
    void rejectsInvalidHash(String value) {
        assertThatThrownBy(() -> new TokenHash(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
