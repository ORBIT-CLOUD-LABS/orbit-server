package com.orbit.server.global.external.cdn;

import com.orbit.server.domain.update.service.DownloadUrlIssuer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * 고른 Edge 의 주소 뒤에 Origin 경로를 붙여 다운로드 URL({@code {Edge 주소}/firmware/{model}/{version}/firmware.bin})을 만든다.
 *
 * <p>다운로드 접근은 내부 대역으로만 제한하므로 아직 서명하지 않는다. 서명 URL 은 #38 에서 이 구현을 바꿔 다룬다.
 */
@Component
class EdgeDownloadUrlIssuer implements DownloadUrlIssuer {

    private static final String TRAILING_SLASHES = "/+$";

    // 차종·버전은 '/' 만 막으므로 '#', '?' 같은 문자가 fragment·쿼리로 해석되지 않게 경로를 인코딩한다. '/' 는 그대로 둔다
    @Override
    public String issue(URI edgeBaseUrl, String path, Instant expiresAt) {
        return withoutTrailingSlashes(edgeBaseUrl) + UriUtils.encodePath(path, StandardCharsets.UTF_8);
    }

    // Origin 경로가 '/' 로 시작하므로 Edge 주소 끝의 '/' 를 떼어 '//' 가 생기지 않게 한다
    private static String withoutTrailingSlashes(URI baseUrl) {
        return baseUrl.toString().replaceFirst(TRAILING_SLASHES, "");
    }
}
