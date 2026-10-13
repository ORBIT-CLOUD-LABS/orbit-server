package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CdnPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(CdnConfig.class);

    @Test
    @DisplayName("DOWNLOAD-REQ-001: 설정한 CDN 주소를 읽는다")
    void bindsBaseUrl() {
        contextRunner
                .withPropertyValues("orbit.cdn.base-url=http://cdn.test")
                .run(context -> assertThat(context.getBean(CdnProperties.class).baseUrl())
                        .isEqualTo(URI.create("http://cdn.test")));
    }

    @Test
    @DisplayName("DOWNLOAD-REQ-001: CDN 주소가 없으면 앱을 띄우지 않는다")
    void failsWithoutBaseUrl() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }
}
