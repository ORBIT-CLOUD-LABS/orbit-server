package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EdgeDownloadUrlIssuerTest {

    private static final String SECRET = "local-url-signing-secret-0123456789";
    private static final String PATH = "/firmware/model-a/1.0.1/firmware.bin";
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-28T11:00:00Z");
    // base64url(MD5("1793185200/firmware/model-a/1.0.1/firmware.bin local-url-signing-secret-0123456789"))
    private static final String SIGNATURE_QUERY = "?md5=yKaKR8G5XPdXXkZKN9QCQA&expires=1793185200&kid=v1";

    private final SecureLinkSigner signer = new SecureLinkSigner(SECRET);
    private final EdgeDownloadUrlIssuer issuer =
            new EdgeDownloadUrlIssuer(signer, new UrlSigningProperties("v1", SECRET));

    @Test
    @DisplayName("DOWNLOAD-POL-001: Edge 주소 뒤에 Origin 경로와 서명(md5), 만료 시각(expires), 키 버전(kid)을 붙인 URL 을 만든다")
    void issuesSignedEdgeUrl() {
        assertThat(issuer.issue(URI.create("http://192.168.100.102:30081"), PATH, EXPIRES_AT))
                .isEqualTo("http://192.168.100.102:30081/firmware/model-a/1.0.1/firmware.bin" + SIGNATURE_QUERY);
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: 서명에 Edge 주소를 넣지 않아 Edge 가 달라도 서명이 같다")
    void signsSameForEveryEdge() {
        assertThat(issuer.issue(URI.create("http://192.168.100.102:30082"), PATH, EXPIRES_AT))
                .isEqualTo("http://192.168.100.102:30082/firmware/model-a/1.0.1/firmware.bin" + SIGNATURE_QUERY);
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: Edge 주소가 '/' 로 끝나도 경로 사이에 '/' 를 하나만 둔다")
    void issuesUrlWithoutDoubleSlash() {
        assertThat(issuer.issue(URI.create("http://192.168.100.102:30081/"), PATH, EXPIRES_AT))
                .isEqualTo("http://192.168.100.102:30081/firmware/model-a/1.0.1/firmware.bin" + SIGNATURE_QUERY);
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: Edge 주소에 하위 경로가 있으면 URL 에는 유지하고 서명은 Origin 경로로만 만든다")
    void keepsBasePathAndSignsOriginPath() {
        assertThat(issuer.issue(URI.create("http://cdn.test/ota"), PATH, EXPIRES_AT))
                .isEqualTo("http://cdn.test/ota/firmware/model-a/1.0.1/firmware.bin" + SIGNATURE_QUERY);
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {"model#A | model%23A", "model?A | model%3FA", "model%A | model%25A", "model A | model%20A"})
    @DisplayName("DOWNLOAD-POL-001: 차종에 URL 예약 문자가 있으면 URL 의 경로는 인코딩하고 서명은 인코딩 전 경로로 만든다")
    void encodesReservedCharactersInPathAndSignsRawPath(String model, String encodedModel) {
        String path = "/firmware/%s/1.0.1/firmware.bin".formatted(model);

        assertThat(issuer.issue(URI.create("http://cdn.test"), path, EXPIRES_AT))
                .isEqualTo("http://cdn.test/firmware/%s/1.0.1/firmware.bin?md5=%s&expires=1793185200&kid=v1"
                        .formatted(encodedModel, signer.sign(path, EXPIRES_AT)));
    }
}
