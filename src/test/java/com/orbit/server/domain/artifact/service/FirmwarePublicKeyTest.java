package com.orbit.server.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FirmwarePublicKeyTest {

    // RFC 8032 7.1 TEST 1: 빈 메시지에 대한 Ed25519 서명. 공개된 시험 벡터라 비밀이 아니다
    private static final String RFC8032_PUBLIC_KEY = "11qYAYKxCrfVS/7TyWQHOg7hcvPapiMlrwIaaPcHURo=";
    private static final byte[] RFC8032_SIGNATURE = HexFormat.of()
            .parseHex(
                    "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b");

    // Ed25519 의 군 위수 L = 2^252 + 27742317777372353535851937790883648493
    private static final BigInteger GROUP_ORDER =
            BigInteger.TWO.pow(252).add(new BigInteger("27742317777372353535851937790883648493"));

    private static final byte[] MESSAGE = "orbit-firmware-v1\nMODEL-A\n1.1.0".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("ORIGIN-POL-001: RFC 8032 시험 벡터의 공개 키(raw 32바이트 base64)로 서명을 검증한다")
    void verifiesRfc8032TestVector() {
        FirmwarePublicKey key = FirmwarePublicKey.fromBase64(RFC8032_PUBLIC_KEY);

        assertThat(key.verifies(new byte[0], RFC8032_SIGNATURE)).isTrue();
        assertThat(key.verifies(new byte[] {1}, RFC8032_SIGNATURE)).isFalse();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 짝이 맞는 개인 키로 만든 서명이면 통과하고, 다른 키의 서명이면 실패한다")
    void verifiesOnlySignatureOfMatchingKey() {
        TestFirmwareSigner signer = new TestFirmwareSigner();
        TestFirmwareSigner otherSigner = new TestFirmwareSigner();
        FirmwarePublicKey key = FirmwarePublicKey.fromBase64(signer.publicKeyBase64());

        assertThat(key.verifies(MESSAGE, decode(signer.sign(MESSAGE)))).isTrue();
        assertThat(key.verifies(MESSAGE, decode(otherSigner.sign(MESSAGE)))).isFalse();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 서명 길이가 64바이트가 아니면 예외 없이 실패로 본다")
    void rejectsWrongLengthSignature() {
        FirmwarePublicKey key = FirmwarePublicKey.fromBase64(RFC8032_PUBLIC_KEY);

        assertThat(key.verifies(new byte[0], Arrays.copyOf(RFC8032_SIGNATURE, 63)))
                .isFalse();
        assertThat(key.verifies(new byte[0], new byte[0])).isFalse();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: S 에 군 위수를 더한 비표준 서명은 같은 서명으로 보지 않고 실패로 본다")
    void rejectsNonCanonicalSignature() {
        FirmwarePublicKey key = FirmwarePublicKey.fromBase64(RFC8032_PUBLIC_KEY);

        assertThat(key.verifies(new byte[0], withScalarPlusGroupOrder(RFC8032_SIGNATURE)))
                .isFalse();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 공개 키가 base64 가 아니면 거부한다")
    void rejectsMalformedBase64() {
        assertThatThrownBy(() -> FirmwarePublicKey.fromBase64("not base64!")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> FirmwarePublicKey.fromBase64(null)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 공개 키가 32바이트가 아니면 거부한다")
    void rejectsWrongLengthKey() {
        String keyOf31Bytes = Base64.getEncoder().encodeToString(new byte[31]);
        String keyOf33Bytes = Base64.getEncoder().encodeToString(new byte[33]);

        assertThatThrownBy(() -> FirmwarePublicKey.fromBase64(keyOf31Bytes)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> FirmwarePublicKey.fromBase64(keyOf33Bytes)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 32바이트여도 Ed25519 곡선 위의 점이 아니면 기동 때 드러나도록 만들 때 거부한다")
    void rejectsKeyNotOnCurve() {
        // y = 2 에 해당하는 x 가 없다
        byte[] notOnCurve = new byte[32];
        notOnCurve[0] = 2;
        // y 가 소수 p(2^255 - 19) 이상이다
        byte[] tooLarge = new byte[32];
        Arrays.fill(tooLarge, (byte) 0xff);
        tooLarge[31] = 0x7f;

        assertThatThrownBy(
                        () -> FirmwarePublicKey.fromBase64(Base64.getEncoder().encodeToString(notOnCurve)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(
                        () -> FirmwarePublicKey.fromBase64(Base64.getEncoder().encodeToString(tooLarge)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 오류 메시지에 공개 키 값을 넣지 않는다")
    void doesNotExposeKeyInMessage() {
        String keyOf31Bytes = Base64.getEncoder().encodeToString(new byte[31]);

        assertThatThrownBy(() -> FirmwarePublicKey.fromBase64(keyOf31Bytes)).hasMessageNotContaining(keyOf31Bytes);
    }

    private static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    // 서명의 뒤 32바이트(S, little-endian)에 L 을 더한다. S + L 도 같은 등식을 만족하지만 RFC 8032 는 S < L 만 받는다
    private static byte[] withScalarPlusGroupOrder(byte[] signature) {
        byte[] scalarBigEndian = reverse(Arrays.copyOfRange(signature, 32, 64));
        byte[] shifted = new BigInteger(1, scalarBigEndian).add(GROUP_ORDER).toByteArray();
        byte[] scalarLittleEndian = Arrays.copyOf(reverse(shifted), 32);
        byte[] result = signature.clone();
        System.arraycopy(scalarLittleEndian, 0, result, 32, 32);
        return result;
    }

    private static byte[] reverse(byte[] bytes) {
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            reversed[i] = bytes[bytes.length - 1 - i];
        }
        return reversed;
    }
}
