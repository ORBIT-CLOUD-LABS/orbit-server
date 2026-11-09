package com.orbit.server.domain.update.dto.response;

import com.orbit.server.domain.edge.service.SelectedEdge;
import com.orbit.server.domain.update.service.EdgeAssignment.FallbackUrl;
import com.orbit.server.domain.update.service.ManifestResult;
import java.util.List;

/**
 * 매니페스트 응답. 차량은 {@code downloadUrl} 로 파일을 받고, 실패하면 {@code fallbackUrls} 를 순서대로 시도하며, 크기와 해시로 검증한다.
 *
 * <p>URL 서명(#38) 전이라 만료 시각({@code urlExpiresAt})은 응답하지 않는다.
 *
 * @param campaignId 캠페인 ID
 * @param targetVersion 설치할 버전
 * @param fileSize 파일 크기(byte)
 * @param sha256 파일의 SHA-256
 * @param edge 다운로드 URL 을 만든 Edge
 * @param downloadUrl 파일을 먼저 받을 Edge 의 URL
 * @param fallbackUrls 예비 Edge 의 URL 목록. 없으면 빈 배열
 */
public record ManifestResponse(
        Long campaignId,
        String targetVersion,
        long fileSize,
        String sha256,
        EdgeResponse edge,
        String downloadUrl,
        List<FallbackUrlResponse> fallbackUrls) {

    public static ManifestResponse from(ManifestResult result) {
        return new ManifestResponse(
                result.campaignId(),
                result.targetVersion(),
                result.fileSize(),
                result.sha256(),
                EdgeResponse.from(result.edge()),
                result.downloadUrl(),
                result.fallbackUrls().stream().map(FallbackUrlResponse::from).toList());
    }

    /**
     * 고른 Edge.
     *
     * @param edgeId Edge id
     * @param distanceKm 차량 위치에서 Edge 까지의 거리(km, 소수 첫째 자리). 차량 위치를 몰라 거리로 고르지 않았으면 null
     */
    public record EdgeResponse(String edgeId, Double distanceKm) {

        private static final double ONE_DECIMAL = 10.0;

        static EdgeResponse from(SelectedEdge edge) {
            Double distanceKm = edge.distanceKm().isPresent()
                    ? roundToOneDecimal(edge.distanceKm().getAsDouble())
                    : null;
            return new EdgeResponse(edge.edgeId(), distanceKm);
        }

        // 거리는 가까운 순서를 보여 주는 참고 값이라 미터 단위 아래까지 내려줄 이유가 없다
        private static double roundToOneDecimal(double value) {
            return Math.round(value * ONE_DECIMAL) / ONE_DECIMAL;
        }
    }

    /**
     * 예비 Edge 의 URL.
     *
     * @param edgeId 예비 Edge 의 id
     * @param url 그 Edge 에서 파일을 받을 URL
     */
    public record FallbackUrlResponse(String edgeId, String url) {

        static FallbackUrlResponse from(FallbackUrl fallback) {
            return new FallbackUrlResponse(fallback.edgeId(), fallback.url());
        }
    }
}
