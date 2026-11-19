package com.orbit.server.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.artifact.service.config.SigningConfig;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SigningPropertiesTest {

    private static final String PUBLIC_KEYS = "orbit.signing.public-keys.";
    private static final byte[] MESSAGE = "payload".getBytes(StandardCharsets.UTF_8);

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(SigningConfig.class);

    private final TestFirmwareSigner signer = new TestFirmwareSigner();

    @Test
    @DisplayName("ORIGIN-POL-001: 키 버전별 공개 키를 읽어 서명 검증에 쓸 공개 키 목록을 만든다")
    void createsPublicKeysFromConfiguration() {
        byte[] signature = Base64.getDecoder().decode(signer.sign(MESSAGE));

        contextRunner
                .withPropertyValues(PUBLIC_KEYS + "v1=" + signer.publicKeyBase64())
                .run(context -> assertThat(context.getBean(FirmwarePublicKeys.class)
                                .find("v1")
                                .orElseThrow()
                                .verifies(MESSAGE, signature))
                        .isTrue());
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 공개 키 설정이 없으면 앱을 띄우지 않는다")
    void failsWithoutPublicKeys() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 공개 키가 32바이트의 base64 가 아니면 앱을 띄우지 않는다")
    void failsWithMalformedPublicKey() {
        String keyOf31Bytes = Base64.getEncoder().encodeToString(new byte[31]);

        contextRunner
                .withPropertyValues(PUBLIC_KEYS + "v1=not-base64!")
                .run(context -> assertThat(context).hasFailed());
        contextRunner
                .withPropertyValues(PUBLIC_KEYS + "v1=" + keyOf31Bytes)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 키 버전이 v1, v2 같은 형식이 아니면 앱을 띄우지 않는다")
    void failsWithMalformedKeyId() {
        contextRunner
                .withPropertyValues(PUBLIC_KEYS + "key1=" + signer.publicKeyBase64())
                .run(context -> assertThat(context).hasFailed());
    }
}
