package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {"model#A | model%23A", "model?A | model%3FA", "model%A | model%25A", "model A | model%20A"})
    @DisplayName("DOWNLOAD-POL-001: 차종에 URL 예약 문자가 있어도 CDN 에 전체 경로가 전달되도록 인코딩한다")
    void encodesReservedCharactersInPath(String model, String encodedModel) {
        String path = "/firmware/%s/1.0.1/firmware.bin".formatted(model);

        assertThat(issuerOf("http://cdn.test").issue(path))
                .isEqualTo("http://cdn.test/firmware/%s/1.0.1/firmware.bin".formatted(encodedModel));
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: 고른 Edge 의 주소 뒤에 Origin 경로를 붙인 URL 을 만든다")
    void issuesEdgeUrlWithOriginPath() {
        assertThat(issuerOf("http://cdn.test").issue(URI.create("http://192.168.100.102:30081"), PATH))
                .isEqualTo("http://192.168.100.102:30081/firmware/model-a/1.0.1/firmware.bin");
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: Edge 주소가 '/' 로 끝나도 경로 사이에 '/' 를 하나만 두고, 예약 문자는 인코딩한다")
    void issuesEdgeUrlWithoutDoubleSlashAndEncoded() {
        String path = "/firmware/model#A/1.0.1/firmware.bin";

        assertThat(issuerOf("http://cdn.test").issue(URI.create("http://192.168.100.102:30081/"), path))
                .isEqualTo("http://192.168.100.102:30081/firmware/model%23A/1.0.1/firmware.bin");
    }

    // 고정 URL 은 CDN 주소만 쓰므로 Edge 관련 값은 비워 둔다
    private static FixedCdnDownloadUrlIssuer issuerOf(String baseUrl) {
        return new FixedCdnDownloadUrlIssuer(new CdnProperties(URI.create(baseUrl), null, List.of()));
    }
}
