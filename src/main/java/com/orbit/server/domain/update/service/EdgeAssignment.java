package com.orbit.server.domain.update.service;

import com.orbit.server.domain.edge.service.SelectedEdge;
import java.time.Instant;
import java.util.List;

/**
 * 차량에 배정한 Edge 와 그 Edge 들에서 파일을 받을 URL.
 *
 * @param edge 다운로드 URL 을 만든 Edge. 거리는 차량 위치를 모르면 빈 값
 * @param downloadUrl 차량이 먼저 시도할 다운로드 URL
 * @param fallbackUrls 다운로드가 실패하면 순서대로 시도할 예비 Edge 의 URL. 없으면 빈 목록
 * @param urlExpiresAt 다운로드 URL 과 예비 URL 이 함께 만료되는 시각
 */
public record EdgeAssignment(
        SelectedEdge edge, String downloadUrl, List<FallbackUrl> fallbackUrls, Instant urlExpiresAt) {

    public EdgeAssignment {
        fallbackUrls = List.copyOf(fallbackUrls);
    }

    /**
     * 예비 Edge 하나의 다운로드 URL.
     *
     * @param edgeId 예비 Edge 의 id
     * @param url 그 Edge 에서 파일을 받을 URL
     */
    public record FallbackUrl(String edgeId, String url) {}
}
