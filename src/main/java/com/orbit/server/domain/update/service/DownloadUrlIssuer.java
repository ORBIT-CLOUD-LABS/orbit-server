package com.orbit.server.domain.update.service;

import java.net.URI;

/**
 * 차량이 업데이트 파일을 CDN Edge 에서 받을 다운로드 URL 을 만든다.
 *
 * <p>CDN 은 외부 시스템이고 Phase2 URL 서명(#38)이 비밀 값으로 이 URL 에 서명을 붙일 예정이므로 인터페이스로 둔다. 구현은
 * global/external 에 둔다.
 */
public interface DownloadUrlIssuer {

    /**
     * Phase1 고정 CDN 주소로 다운로드 URL 을 만든다. Edge 기준 URL 로 전환한 뒤 제거한다.
     *
     * @param path Origin 경로. 예: {@code /firmware/{model}/{version}/firmware.bin}
     * @return 차량이 인증 헤더 없이 파일을 받을 URL
     * @deprecated Edge 를 골라 {@link #issue(URI, String)} 를 쓴다
     */
    @Deprecated(forRemoval = true)
    String issue(String path);

    /**
     * 고른 Edge 에서 Origin 경로의 파일을 받을 다운로드 URL 을 만든다. Edge 마다 경로가 같으므로 주소만 바뀐다.
     *
     * @param edgeBaseUrl 고른 Edge 의 주소. 예: {@code http://192.168.100.102:30081}
     * @param path Origin 경로. 예: {@code /firmware/{model}/{version}/firmware.bin}
     * @return 차량이 인증 헤더 없이 파일을 받을 URL
     */
    String issue(URI edgeBaseUrl, String path);
}
