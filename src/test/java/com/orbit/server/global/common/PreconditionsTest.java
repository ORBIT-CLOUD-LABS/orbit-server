package com.orbit.server.global.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PreconditionsTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("필수 문자열이 비어 있으면 거부한다")
    void rejectsBlankText(String value) {
        assertThatThrownBy(() -> Preconditions.requireText(value, 10, "값"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최대 길이를 넘으면 거부한다")
    void rejectsTooLongText() {
        assertThatThrownBy(() -> Preconditions.requireText("12345678901", 10, "값"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최대 길이와 같으면 허용한다")
    void acceptsTextAtMaxLength() {
        assertThat(Preconditions.requireText("1234567890", 10, "값")).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("선택 문자열은 null 을 허용한다")
    void acceptsNullOptionalText() {
        assertThat(Preconditions.requireMaxLength(null, 10, "값")).isNull();
    }
}
