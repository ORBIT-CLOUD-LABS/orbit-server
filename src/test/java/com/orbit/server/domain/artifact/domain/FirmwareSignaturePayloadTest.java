package com.orbit.server.domain.artifact.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FirmwareSignaturePayloadTest {

    private static final String SHA256 = "a80ed4b4" + "0".repeat(56);
    private static final ArtifactRelease RELEASE = new ArtifactRelease("MODEL-A", "1.1.0");
    private static final ArtifactFile FILE = new ArtifactFile(SHA256, 20971520, RELEASE.originPath());

    @Test
    @DisplayName("ORIGIN-POL-001: 다섯 줄을 \\n 으로 잇고 끝에 줄바꿈을 붙이지 않는다")
    void joinsFiveLinesWithoutTrailingNewline() {
        // printf 'orbit-firmware-v1\n%s\n%s\n%s\n%s' "$MODEL" "$VERSION" "$SHA256" "$SIZE" 와 같은 문자열
        String expected = "orbit-firmware-v1\nMODEL-A\n1.1.0\n" + SHA256 + "\n20971520";

        byte[] payload = new FirmwareSignaturePayload(RELEASE, FILE).toBytes();

        assertThat(new String(payload, StandardCharsets.UTF_8)).isEqualTo(expected);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 버전은 등록한 표기 그대로 넣어 1.0 과 1.0.0 은 다른 서명 대상이 된다")
    void keepsVersionAsRegistered() {
        ArtifactRelease shortVersion = new ArtifactRelease("MODEL-A", "1.0");
        ArtifactRelease longVersion = new ArtifactRelease("MODEL-A", "1.0.0");

        byte[] shortPayload = new FirmwareSignaturePayload(shortVersion, FILE).toBytes();
        byte[] longPayload = new FirmwareSignaturePayload(longVersion, FILE).toBytes();

        assertThat(shortPayload).isNotEqualTo(longPayload);
    }
}
