package com.orbit.server.domain.update.service;

/**
 * 차량이 업데이트 파일을 CDN 에서 받을 다운로드 URL 을 만든다. (MANIFEST-REQ-001, DOWNLOAD-REQ-001 / DOWNLOAD-POL-001)
 *
 * <p>CDN 은 외부 시스템이고 Phase1 고정 URL 을 Phase2 에서 서명 URL 로 바꿀 예정이므로 인터페이스로 둔다. 구현은 global/external 에
 * 둔다.
 */
public interface DownloadUrlIssuer {

    /**
     * Origin 경로의 파일을 받을 다운로드 URL 을 만든다.
     *
     * @param path Origin 경로. 예: {@code /firmware/{model}/{version}/firmware.bin}
     * @return 차량이 인증 헤더 없이 파일을 받을 URL
     */
    String issue(String path);
}
