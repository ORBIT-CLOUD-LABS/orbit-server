package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.count.EdgeDownloadCount;

/**
 * [Phase2] 한 Edge 에서 파일을 받은 차량의 한 캠페인 업데이트 성공·실패 수.
 *
 * @param campaignId 캠페인 ID
 * @param succeeded 업데이트에 성공한 차량 수
 * @param failed 업데이트에 실패한 차량 수
 */
public record CdnEdgeDownloadResponse(Long campaignId, long succeeded, long failed) {

    /**
     * @param count 조회한 Edge·캠페인별 성공·실패 수
     * @return 같은 값을 담은 응답. Edge 는 감싸는 응답이 알려 주므로 담지 않는다
     */
    public static CdnEdgeDownloadResponse from(EdgeDownloadCount count) {
        return new CdnEdgeDownloadResponse(count.campaignId(), count.succeeded(), count.failed());
    }
}
