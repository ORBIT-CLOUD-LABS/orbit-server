package com.orbit.server.domain.update.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SignedUrlPropertiesTest {

    private static final String TTL = "orbit.cdn.signed-url-ttl=";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(SignedUrlConfig.class);

    @Test
    @DisplayName("DOWNLOAD-POL-001: 설정한 URL 서명 유효 기간을 읽는다")
    void bindsTtl() {
        contextRunner
                .withPropertyValues(TTL + "1h")
                .run(context -> assertThat(
                                context.getBean(SignedUrlProperties.class).signedUrlTtl())
                        .isEqualTo(Duration.ofHours(1)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s", "999ms"})
    @DisplayName("DOWNLOAD-POL-001: 유효 기간이 1초보다 짧으면 앱을 띄우지 않는다")
    void failsWithTtlShorterThanOneSecond(String ttl) {
        contextRunner
                .withPropertyValues(TTL + ttl)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: 유효 기간을 설정하지 않으면 앱을 띄우지 않는다")
    void failsWithoutTtl() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: 만료 시각은 발급 시각에 유효 기간을 더한 시각이다")
    void expiresAfterTtl() {
        SignedUrlProperties properties = new SignedUrlProperties(Duration.ofHours(1));

        assertThat(properties.expiresAt(Instant.parse("2026-10-28T10:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-28T11:00:00Z"));
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: Edge 가 초 단위로 만료를 보므로 만료 시각의 초 아래는 버린다")
    void truncatesExpiryToSeconds() {
        SignedUrlProperties properties = new SignedUrlProperties(Duration.ofHours(1));

        assertThat(properties.expiresAt(Instant.parse("2026-10-28T10:00:00.999Z")))
                .isEqualTo(Instant.parse("2026-10-28T11:00:00Z"));
    }
}
