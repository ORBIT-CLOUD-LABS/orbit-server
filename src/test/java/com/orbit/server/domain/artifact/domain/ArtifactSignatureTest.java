package com.orbit.server.domain.artifact.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ArtifactSignatureTest {

    private static final byte[] SIGNATURE_BYTES = new byte[64];
    private static final String SIGNATURE = Base64.getEncoder().encodeToString(SIGNATURE_BYTES);

    @Test
    @DisplayName("ORIGIN-POL-001: 64바이트 서명의 base64 와 키 버전으로 생성하고 서명 바이트를 돌려준다")
    void createsWithValidValues() {
        ArtifactSignature signature = new ArtifactSignature(SIGNATURE, "v1");

        assertThat(signature.signatureBytes()).isEqualTo(SIGNATURE_BYTES);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("ORIGIN-POL-001: 서명이 없으면 거부한다")
    void rejectsMissingSignature(String signature) {
        assertThatThrownBy(() -> new ArtifactSignature(signature, "v1")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 64바이트가 아닌 서명은 거부한다")
    void rejectsWrongLengthSignature() {
        String signatureOf63Bytes = Base64.getEncoder().encodeToString(new byte[63]);
        String signatureOf65Bytes = Base64.getEncoder().encodeToString(new byte[65]);

        assertThatThrownBy(() -> new ArtifactSignature(signatureOf63Bytes, "v1"))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ArtifactSignature(signatureOf65Bytes, "v1"))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: base64 문자가 아니거나 패딩이 빠진 서명은 거부한다")
    void rejectsMalformedBase64() {
        String urlSafe = "-".repeat(86) + "==";
        String withoutPadding = SIGNATURE.substring(0, 86);

        assertThatThrownBy(() -> new ArtifactSignature(urlSafe, "v1")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ArtifactSignature(withoutPadding, "v1")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 남는 비트가 0 이 아닌 비표준 base64 표기는 거부한다")
    void rejectsNonCanonicalBase64() {
        // 표준 표기는 "AAAA...AA==" 이고, 마지막 글자 B 는 버려지는 비트에 1 이 있어 같은 바이트로 디코딩된다
        String nonCanonical = "A".repeat(85) + "B==";

        assertThatThrownBy(() -> new ArtifactSignature(nonCanonical, "v1")).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1", "V1", "v0", "v01", "key-1", "v123456789012345678901"})
    @DisplayName("ORIGIN-POL-001: 키 버전이 v1, v2 같은 형식이 아니면 거부한다")
    void rejectsMalformedKeyId(String keyId) {
        assertThatThrownBy(() -> new ArtifactSignature(SIGNATURE, keyId)).isInstanceOf(InvalidValueException.class);
        assertThat(ArtifactSignature.isKeyId(keyId)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "v2", "v10"})
    @DisplayName("ORIGIN-POL-001: Vault 키 버전 형식이면 키 버전으로 본다")
    void acceptsKeyId(String keyId) {
        assertThat(ArtifactSignature.isKeyId(keyId)).isTrue();
    }
}
