package com.orbit.server.domain.artifact.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.artifact.service.FirmwarePublicKeys;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** 설정 바인딩과 기동 실패를 본다. 공개 키로 서명이 검증되는지는 {@code FirmwarePublicKeysTest} 가 본다. */
class SigningPropertiesTest {

    private static final String PUBLIC_KEYS = "orbit.signing.public-keys.";
    // RFC 8032 Ed25519 테스트 벡터 1 의 공개 키(raw 32바이트)를 base64 로 쓴 것
    private static final String VALID_PUBLIC_KEY = "11qYAYKxCrfVS/7TyWQHOg7hcvPapiMlrwIaaPcHURo=";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(SigningConfig.class);

    @Test
    @DisplayName("ORIGIN-POL-001: 키 버전별 공개 키가 올바르면 서명 검증에 쓸 공개 키 목록을 만든다")
    void createsPublicKeysWithValidPublicKey() {
        contextRunner
                .withPropertyValues(PUBLIC_KEYS + "v1=" + VALID_PUBLIC_KEY)
                .run(context -> assertThat(context).hasSingleBean(FirmwarePublicKeys.class));
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
                .withPropertyValues(PUBLIC_KEYS + "key1=" + VALID_PUBLIC_KEY)
                .run(context -> assertThat(context).hasFailed());
    }
}
