package com.orbit.server.domain.update.service;

import java.net.URI;
import java.time.Instant;

/**
 * 차량이 업데이트 파일을 CDN Edge 에서 받을 다운로드 URL 을 만든다.
 *
 * <p>CDN 은 외부 시스템이고 Edge 와 함께 쓰는 비밀 값으로 이 URL 에 서명을 붙이므로 인터페이스로 둔다. 구현은 global/external 에 둔다.
 */
public interface DownloadUrlIssuer {

    /**
     * 고른 Edge 에서 Origin 경로의 파일을 받을 다운로드 URL 을 만든다. Edge 마다 경로가 같으므로 주소만 바뀐다.
     *
     * @param edgeBaseUrl 고른 Edge 의 주소. 예: {@code http://192.168.100.102:30081}
     * @param path Origin 경로. 예: {@code /firmware/{model}/{version}/firmware.bin}
     * @param expiresAt URL 이 만료되는 시각. 이 시각이 지나면 Edge 가 다운로드를 거부한다
     * @return 차량이 인증 헤더 없이 만료 시각까지 파일을 받을 URL
     */
    String issue(URI edgeBaseUrl, String path, Instant expiresAt);
}
