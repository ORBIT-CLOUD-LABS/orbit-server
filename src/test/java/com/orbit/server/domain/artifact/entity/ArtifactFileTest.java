package com.orbit.server.domain.artifact.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArtifactFileTest {

    private static final String SHA256 = "a".repeat(64);
    private static final String PATH = "/firmware/model-a/1.0.0/firmware.bin";

    @Test
    @DisplayName("ORIGIN-REQ-001: 올바른 해시·크기·경로로 생성한다")
    void createsWithValidValues() {
        assertThatCode(() -> new ArtifactFile(SHA256, 1, PATH)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    @DisplayName("ORIGIN-REQ-001: SHA-256 이 소문자 16진수 64자가 아니면 거부한다")
    void rejectsInvalidSha256(String sha256) {
        assertThatThrownBy(() -> new ArtifactFile(sha256, 1, PATH)).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    @DisplayName("ORIGIN-REQ-001: 파일 크기가 0 이하면 거부한다")
    void rejectsNonPositiveSize(long sizeBytes) {
        assertThatThrownBy(() -> new ArtifactFile(SHA256, sizeBytes, PATH)).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            })
    @DisplayName("ORIGIN-REQ-001: 요청 해시가 파일 해시와 대소문자만 달라도 일치로 본다")
    void matchesSameSha256IgnoringCase(String expectedSha256) {
        ArtifactFile file = new ArtifactFile(SHA256, 1, PATH);

        assertThat(file.matchesSha256(expectedSha256)).isTrue();
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 요청 해시가 파일 해시와 다르면 불일치로 본다")
    void doesNotMatchDifferentSha256() {
        ArtifactFile file = new ArtifactFile(SHA256, 1, PATH);

        assertThat(file.matchesSha256("b".repeat(64))).isFalse();
    }
}
