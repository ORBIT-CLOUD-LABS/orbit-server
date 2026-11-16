package com.orbit.server.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FirmwarePublicKeysTest {

    private static final byte[] MESSAGE = "payload".getBytes(StandardCharsets.UTF_8);

    private final TestFirmwareSigner signer = new TestFirmwareSigner();

    @Test
    @DisplayName("ORIGIN-POL-001: 키 버전으로 공개 키를 찾는다")
    void findsKeyById() {
        TestFirmwareSigner otherSigner = new TestFirmwareSigner();
        FirmwarePublicKeys keys = FirmwarePublicKeys.fromBase64(
                Map.of("v1", signer.publicKeyBase64(), "v2", otherSigner.publicKeyBase64()));

        assertThat(keys.find("v1"))
                .hasValueSatisfying(key -> assertThat(key.verifies(MESSAGE, decode(signer.sign(MESSAGE))))
                        .isTrue());
        assertThat(keys.find("v2"))
                .hasValueSatisfying(key -> assertThat(key.verifies(MESSAGE, decode(otherSigner.sign(MESSAGE))))
                        .isTrue());
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 모르는 키 버전이면 빈 값이다")
    void returnsEmptyForUnknownKeyId() {
        FirmwarePublicKeys keys = FirmwarePublicKeys.fromBase64(Map.of("v1", signer.publicKeyBase64()));

        assertThat(keys.find("v2")).isEmpty();
        assertThat(keys.find(null)).isEmpty();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 공개 키가 하나도 없으면 거부한다")
    void rejectsEmptyKeys() {
        assertThatThrownBy(() -> FirmwarePublicKeys.fromBase64(Map.of())).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> FirmwarePublicKeys.fromBase64(null)).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "V1", "key"})
    @DisplayName("ORIGIN-POL-001: 키 버전이 v1, v2 같은 형식이 아니면 거부한다")
    void rejectsMalformedKeyId(String keyId) {
        Map<String, String> keys = Map.of(keyId, signer.publicKeyBase64());

        assertThatThrownBy(() -> FirmwarePublicKeys.fromBase64(keys)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 공개 키 하나라도 잘못되면 어느 키 버전인지 알리고 키 값은 알리지 않는다")
    void rejectsMalformedKeyWithKeyIdOnly() {
        String keyOf31Bytes = Base64.getEncoder().encodeToString(new byte[31]);
        Map<String, String> keys = Map.of("v1", signer.publicKeyBase64(), "v2", keyOf31Bytes);

        assertThatThrownBy(() -> FirmwarePublicKeys.fromBase64(keys))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("v2")
                .hasMessageNotContaining(keyOf31Bytes);
    }

    private static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}
