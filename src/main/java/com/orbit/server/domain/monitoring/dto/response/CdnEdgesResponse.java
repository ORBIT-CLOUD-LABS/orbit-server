package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.result.CdnEdgeStatus;
import java.util.List;

/**
 * [Phase2] 대시보드 CDN Edge 상태 응답.
 *
 * @param edges 설정의 모든 Edge 의 상태. 설정 순서
 */
public record CdnEdgesResponse(List<CdnEdgeStatusResponse> edges) {

    /**
     * @param statuses 조회한 Edge 상태. 설정 순서
     * @return 같은 값을 같은 순서로 담은 응답
     */
    public static CdnEdgesResponse from(List<CdnEdgeStatus> statuses) {
        return new CdnEdgesResponse(
                statuses.stream().map(CdnEdgeStatusResponse::from).toList());
    }
}
