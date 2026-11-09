package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EdgeDownloadUrlIssuerTest {

    private static final String PATH = "/firmware/model-a/1.0.1/firmware.bin";

    private final EdgeDownloadUrlIssuer issuer = new EdgeDownloadUrlIssuer();

    @Test
    @DisplayName("DOWNLOAD-POL-001: Edge 주소 뒤에 Origin 경로를 붙인 URL 을 만든다")
    void issuesEdgeUrlWithOriginPath() {
        assertThat(issuer.issue(URI.create("http://192.168.100.102:30081"), PATH))
                .isEqualTo("http://192.168.100.102:30081/firmware/model-a/1.0.1/firmware.bin");
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: Edge 주소가 '/' 로 끝나도 경로 사이에 '/' 를 하나만 둔다")
    void issuesUrlWithoutDoubleSlash() {
        assertThat(issuer.issue(URI.create("http://192.168.100.102:30081/"), PATH))
                .isEqualTo("http://192.168.100.102:30081/firmware/model-a/1.0.1/firmware.bin");
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: Edge 주소에 하위 경로가 있으면 유지한다")
    void keepsBasePath() {
        assertThat(issuer.issue(URI.create("http://cdn.test/ota"), PATH))
                .isEqualTo("http://cdn.test/ota/firmware/model-a/1.0.1/firmware.bin");
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {"model#A | model%23A", "model?A | model%3FA", "model%A | model%25A", "model A | model%20A"})
    @DisplayName("DOWNLOAD-POL-001: 차종에 URL 예약 문자가 있어도 Edge 에 전체 경로가 전달되도록 인코딩한다")
    void encodesReservedCharactersInPath(String model, String encodedModel) {
        String path = "/firmware/%s/1.0.1/firmware.bin".formatted(model);

        assertThat(issuer.issue(URI.create("http://cdn.test"), path))
                .isEqualTo("http://cdn.test/firmware/%s/1.0.1/firmware.bin".formatted(encodedModel));
    }
}
