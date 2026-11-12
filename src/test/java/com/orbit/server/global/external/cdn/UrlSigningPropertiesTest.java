package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;

class UrlSigningPropertiesTest {

    private static final String KEY_ID = "orbit.cdn.url-signing.key-id=";
    private static final String SECRET = "orbit.cdn.url-signing.secret=";
    private static final String VALID_SECRET = "local-url-signing-secret-0123456789";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(UrlSigningConfig.class);

    @Test
    @DisplayName("설정한 키 버전과 비밀 값을 읽는다")
    void bindsKeyIdAndSecret() {
        contextRunner
                .withPropertyValues(KEY_ID + "v1", SECRET + VALID_SECRET)
                .run(context -> assertThat(context.getBean(UrlSigningProperties.class))
                        .isEqualTo(new UrlSigningProperties("v1", VALID_SECRET)));
    }

    @Test
    @DisplayName("설정한 비밀 값으로 서명하는 서명 계산기를 만든다")
    void createsSignerWithConfiguredSecret() {
        String path = "/firmware/MODEL-A/1.1.0/firmware.bin";
        Instant expiresAt = Instant.ofEpochSecond(1761649200);

        contextRunner
                .withPropertyValues(KEY_ID + "v1", SECRET + VALID_SECRET)
                .run(context -> assertThat(
                                context.getBean(SecureLinkSigner.class).sign(path, expiresAt))
                        .isEqualTo(new SecureLinkSigner(VALID_SECRET).sign(path, expiresAt)));
    }

    @Test
    @DisplayName("로그나 예외 메시지에 남지 않도록 문자열로 바꿀 때 비밀 값을 가린다")
    void masksSecretInToString() {
        UrlSigningProperties properties = new UrlSigningProperties("v1", VALID_SECRET);

        assertThat(properties.toString()).contains("v1").doesNotContain(VALID_SECRET);
    }

    @Test
    @DisplayName("비밀 값이 없으면 앱을 띄우지 않는다")
    void failsWithoutSecret() {
        contextRunner
                .withPropertyValues(KEY_ID + "v1")
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short-secret", "0123456789012345678901234567890"})
    @DisplayName("비밀 값이 32자보다 짧으면 앱을 띄우지 않는다")
    void failsWithShortSecret(String secret) {
        contextRunner
                .withPropertyValues(KEY_ID + "v1", SECRET + secret)
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                VALID_SECRET + "\n",
                VALID_SECRET + " ",
                " " + VALID_SECRET,
                "local-url-signing secret-0123456789"
            })
    @DisplayName("비밀 값에 공백이나 개행이 섞이면 Edge 의 비밀 값과 달라져 서명이 맞지 않으므로 앱을 띄우지 않는다")
    void failsWithWhitespaceInSecret(String secret) {
        // withPropertyValues 는 값 앞뒤 공백을 떼어 내므로, 환경 변수처럼 값을 그대로 전달하도록 프로퍼티 소스를 직접 넣는다
        Map<String, Object> properties =
                Map.of("orbit.cdn.url-signing.key-id", "v1", "orbit.cdn.url-signing.secret", secret);

        contextRunner
                .withInitializer(context -> context.getEnvironment()
                        .getPropertySources()
                        .addFirst(new MapPropertySource("untrimmed", properties)))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("키 버전이 없으면 앱을 띄우지 않는다")
    void failsWithoutKeyId() {
        contextRunner
                .withPropertyValues(SECRET + VALID_SECRET)
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "v 1", "v1&md5=x", "v/1", "키"})
    @DisplayName("키 버전이 비었거나 쿼리에 그대로 넣을 수 없는 문자를 담으면 앱을 띄우지 않는다")
    void failsWithUnsafeKeyId(String keyId) {
        contextRunner
                .withPropertyValues(KEY_ID + keyId, SECRET + VALID_SECRET)
                .run(context -> assertThat(context).hasFailed());
    }
}
