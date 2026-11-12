package com.orbit.server.global.external.cdn;

import com.orbit.server.domain.update.service.DownloadUrlIssuer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * 고른 Edge 의 주소 뒤에 Origin 경로와 URL 서명을 붙여 다운로드 URL({@code {Edge
 * 주소}/firmware/{model}/{version}/firmware.bin?md5=…&expires=…&kid=…})을 만든다.
 *
 * <p>Edge 는 {@code kid} 로 비밀 값을 고르고 {@code md5} 와 {@code expires} 를 nginx {@code secure_link} 로 검증한다. 서명이 틀리면
 * 403, 만료됐으면 410 이다.
 *
 * <p>서명에는 Edge 주소를 넣지 않고 Origin 경로만 넣어 같은 서명이 모든 Edge 에서 통하게 한다. 그래서 Edge 주소에 하위 경로가 있으면 그
 * Edge 는 하위 경로를 뺀 Origin 경로로 검증해야 한다.
 */
@Component
@RequiredArgsConstructor
class EdgeDownloadUrlIssuer implements DownloadUrlIssuer {

    private static final String TRAILING_SLASHES = "/+$";
    private static final String SIGNED_URL_FORMAT = "%s%s?md5=%s&expires=%d&kid=%s";

    private final SecureLinkSigner signer;
    private final UrlSigningProperties properties;

    // 차종·버전은 '/' 만 막으므로 '#', '?' 같은 문자가 fragment·쿼리로 해석되지 않게 경로를 인코딩한다. '/' 는 그대로 둔다.
    // Edge 는 디코딩한 경로로 검증하므로 서명은 인코딩하기 전 경로로 만든다
    @Override
    public String issue(URI edgeBaseUrl, String path, Instant expiresAt) {
        return SIGNED_URL_FORMAT.formatted(
                withoutTrailingSlashes(edgeBaseUrl),
                UriUtils.encodePath(path, StandardCharsets.UTF_8),
                signer.sign(path, expiresAt),
                expiresAt.getEpochSecond(),
                properties.keyId());
    }

    // Origin 경로가 '/' 로 시작하므로 Edge 주소 끝의 '/' 를 떼어 '//' 가 생기지 않게 한다
    private static String withoutTrailingSlashes(URI baseUrl) {
        return baseUrl.toString().replaceFirst(TRAILING_SLASHES, "");
    }
}
