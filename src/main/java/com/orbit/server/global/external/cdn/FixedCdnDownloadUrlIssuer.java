package com.orbit.server.global.external.cdn;

import com.orbit.server.domain.update.service.DownloadUrlIssuer;
import java.net.URI;
import org.springframework.stereotype.Component;

/**
 * Phase1 다운로드 URL. 파일마다 고정된 CDN URL({@code {CDN 주소}/firmware/{model}/{version}/firmware.bin})을 만든다.
 * (DOWNLOAD-REQ-001 / DOWNLOAD-POL-001)
 *
 * <p>Phase1 은 다운로드 접근을 내부 대역으로만 제한하므로 서명하지 않는다. 서명 URL 은 Phase2(#38)에서 이 구현을 바꿔 다룬다.
 */
@Component
class FixedCdnDownloadUrlIssuer implements DownloadUrlIssuer {

    private static final String TRAILING_SLASHES = "/+$";

    private final String baseUrl;

    FixedCdnDownloadUrlIssuer(CdnProperties properties) {
        this.baseUrl = withoutTrailingSlashes(properties.baseUrl());
    }

    @Override
    public String issue(String path) {
        return baseUrl + path;
    }

    // Origin 경로가 '/' 로 시작하므로 설정한 주소 끝의 '/' 를 떼어 '//' 가 생기지 않게 한다
    private static String withoutTrailingSlashes(URI baseUrl) {
        return baseUrl.toString().replaceFirst(TRAILING_SLASHES, "");
    }
}
