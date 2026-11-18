package com.orbit.server.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.domain.artifact.entity.ArtifactFile;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.artifact.entity.ArtifactSignature;
import com.orbit.server.domain.artifact.entity.FirmwareSignaturePayload;
import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.global.error.BusinessException;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class FirmwareSignatureVerifierTest {

    private static final String SHA256 = "a".repeat(64);
    private static final ArtifactRelease RELEASE = new ArtifactRelease("MODEL-A", "1.1.0");
    private static final FirmwareSignaturePayload PAYLOAD =
            new FirmwareSignaturePayload(RELEASE, new ArtifactFile(SHA256, 1024, RELEASE.originPath()));

    private final TestFirmwareSigner signer = new TestFirmwareSigner();
    private final FirmwareSignatureVerifier verifier =
            new FirmwareSignatureVerifier(FirmwarePublicKeys.fromBase64(Map.of("v1", signer.publicKeyBase64())));

    @Test
    @DisplayName("ORIGIN-POL-001: 키 버전의 공개 키로 서명이 맞으면 저장할 서명을 돌려준다")
    void returnsVerifiedSignature() {
        String signature = signer.sign(PAYLOAD.toBytes());

        ArtifactSignature verified = parseAndVerify(PAYLOAD, signature, "v1");

        assertThat(verified).isEqualTo(new ArtifactSignature(signature, "v1"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("ORIGIN-POL-001: 서명이 없으면 422 SIGNATURE_INVALID")
    void rejectsMissingSignature(String signature) {
        assertSignatureInvalid(() -> verifier.parse(signature, "v1"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("ORIGIN-POL-001: 키 버전이 없으면 422 SIGNATURE_INVALID")
    void rejectsMissingKeyId(String keyId) {
        String signature = signer.sign(PAYLOAD.toBytes());

        assertSignatureInvalid(() -> verifier.parse(signature, keyId));
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 서명이 64바이트의 base64 가 아니면 422 SIGNATURE_INVALID")
    void rejectsMalformedSignature() {
        String signatureOf32Bytes = Base64.getEncoder().encodeToString(new byte[32]);

        assertSignatureInvalid(() -> verifier.parse(signatureOf32Bytes, "v1"));
        assertSignatureInvalid(() -> verifier.parse("not base64!", "v1"));
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 설정에 없는 키 버전이면 422 SIGNATURE_INVALID")
    void rejectsUnknownKeyId() {
        String signature = signer.sign(PAYLOAD.toBytes());

        assertSignatureInvalid(() -> verifier.parse(signature, "v2"));
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 다른 개인 키로 만든 서명이면 422 SIGNATURE_INVALID")
    void rejectsSignatureOfOtherKey() {
        String signature = new TestFirmwareSigner().sign(PAYLOAD.toBytes());

        assertSignatureInvalid(() -> parseAndVerify(PAYLOAD, signature, "v1"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tamperedPayloads")
    @DisplayName("ORIGIN-POL-001: 서명 대상의 필드 하나만 달라도 422 SIGNATURE_INVALID")
    void rejectsSignatureOfDifferentPayload(String changedField, FirmwareSignaturePayload tampered) {
        String signature = signer.sign(PAYLOAD.toBytes());

        assertSignatureInvalid(() -> parseAndVerify(tampered, signature, "v1"));
    }

    static Stream<Arguments> tamperedPayloads() {
        ArtifactRelease otherModel = new ArtifactRelease("MODEL-B", "1.1.0");
        ArtifactRelease otherVersion = new ArtifactRelease("MODEL-A", "1.1.1");
        // 비교 규칙상 같은 버전이어도 표기가 다르면 다른 서명이다
        ArtifactRelease sameVersionOtherNotation = new ArtifactRelease("MODEL-A", "1.1.0.0");
        return Stream.of(
                Arguments.of(
                        "model",
                        new FirmwareSignaturePayload(
                                otherModel, new ArtifactFile(SHA256, 1024, otherModel.originPath()))),
                Arguments.of(
                        "version",
                        new FirmwareSignaturePayload(
                                otherVersion, new ArtifactFile(SHA256, 1024, otherVersion.originPath()))),
                Arguments.of(
                        "version notation",
                        new FirmwareSignaturePayload(
                                sameVersionOtherNotation,
                                new ArtifactFile(SHA256, 1024, sameVersionOtherNotation.originPath()))),
                Arguments.of(
                        "sha256",
                        new FirmwareSignaturePayload(
                                RELEASE, new ArtifactFile("b".repeat(64), 1024, RELEASE.originPath()))),
                Arguments.of(
                        "fileSize",
                        new FirmwareSignaturePayload(RELEASE, new ArtifactFile(SHA256, 1025, RELEASE.originPath()))));
    }

    // 등록 서비스와 같은 순서로 형식·키 버전을 확인한 뒤 서명 대상과 대조한다
    private ArtifactSignature parseAndVerify(FirmwareSignaturePayload payload, String signature, String keyId) {
        ArtifactSignature parsed = verifier.parse(signature, keyId);
        verifier.verify(payload, parsed);
        return parsed;
    }

    private static void assertSignatureInvalid(ThrowingCallable verification) {
        assertThatThrownBy(verification)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.SIGNATURE_INVALID);
    }
}
