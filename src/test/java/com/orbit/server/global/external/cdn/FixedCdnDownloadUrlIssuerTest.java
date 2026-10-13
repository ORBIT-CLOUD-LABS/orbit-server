package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FixedCdnDownloadUrlIssuerTest {

    private static final String PATH = "/firmware/model-a/1.0.1/firmware.bin";

    @Test
    @DisplayName("DOWNLOAD-POL-001: CDN 주소 뒤에 Origin 경로를 붙인 고정 URL 을 만든다")
    void issuesCdnUrlWithOriginPath() {
        assertThat(issuerOf("http://cdn.test").issue(PATH))
                .isEqualTo("http://cdn.test/firmware/model-a/1.0.1/firmware.bin");
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: CDN 주소가 '/' 로 끝나도 경로 사이에 '/' 를 하나만 둔다")
    void issuesUrlWithoutDoubleSlash() {
        assertThat(issuerOf("http://cdn.test/").issue(PATH))
                .isEqualTo("http://cdn.test/firmware/model-a/1.0.1/firmware.bin");
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: CDN 주소에 하위 경로가 있으면 유지한다")
    void keepsBasePath() {
        assertThat(issuerOf("http://cdn.test/ota").issue(PATH))
                .isEqualTo("http://cdn.test/ota/firmware/model-a/1.0.1/firmware.bin");
    }

    private static FixedCdnDownloadUrlIssuer issuerOf(String baseUrl) {
        return new FixedCdnDownloadUrlIssuer(new CdnProperties(URI.create(baseUrl)));
    }
}
