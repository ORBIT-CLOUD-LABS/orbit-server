package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecureLinkSignerTest {

    private static final String SECRET = "local-url-signing-secret-0123456789";
    private static final String PATH = "/firmware/MODEL-A/1.1.0/firmware.bin";
    private static final long EXPIRES = 1761649200L;

    private final SecureLinkSigner signer = new SecureLinkSigner(SECRET);

    @Test
    @DisplayName("DOWNLOAD-POL-001: nginx secure_link 문서의 예시와 같은 서명을 만든다")
    void signsLikeNginxSecureLinkExample() {
        // nginx 문서 예시: echo -n '2147483647/s/link127.0.0.1 secret' | openssl md5 -binary | openssl base64 | tr +/ -_ |
        // tr -d =
        SecureLinkSigner nginxExample = new SecureLinkSigner("secret");

        assertThat(nginxExample.sign("/s/link127.0.0.1", 2147483647L)).isEqualTo("_e4Nc3iduzkWRm01TBBNYw");
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: 만료 시각(Unix 초), 경로, 비밀 값으로 서명을 만든다")
    void signsExpiresPathAndSecret() {
        assertThat(signer.sign(PATH, EXPIRES)).isEqualTo("3eyiqoGxo0havdsctw4IQg");
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: 서명은 쿼리에 그대로 넣을 수 있게 base64url 로 만들고 패딩을 붙이지 않는다")
    void signsInUrlSafeBase64WithoutPadding() {
        assertThat(signer.sign(PATH, EXPIRES)).matches("[A-Za-z0-9_-]{22}");
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: 만료 시각이 다르면 서명이 달라진다")
    void signsDifferentlyForDifferentExpiry() {
        assertThat(signer.sign(PATH, EXPIRES + 1)).isNotEqualTo(signer.sign(PATH, EXPIRES));
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: 경로가 다르면 서명이 달라진다")
    void signsDifferentlyForDifferentPath() {
        assertThat(signer.sign("/firmware/MODEL-A/1.2.0/firmware.bin", EXPIRES))
                .isNotEqualTo(signer.sign(PATH, EXPIRES));
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: 비밀 값이 다르면 서명이 달라진다")
    void signsDifferentlyForDifferentSecret() {
        SecureLinkSigner other = new SecureLinkSigner("another-url-signing-secret-0123456789");

        assertThat(other.sign(PATH, EXPIRES)).isNotEqualTo(signer.sign(PATH, EXPIRES));
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: Edge 가 디코딩한 경로로 검증하므로 인코딩하지 않은 경로 그대로 서명한다")
    void signsRawPath() {
        assertThat(signer.sign("/firmware/model A/1.1.0/firmware.bin", EXPIRES))
                .isNotEqualTo(signer.sign("/firmware/model%20A/1.1.0/firmware.bin", EXPIRES));
    }
}
