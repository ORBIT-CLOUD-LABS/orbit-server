package com.orbit.server.domain.monitoring.service.count;

import java.util.Objects;

/**
 * 한 Edge 에서 파일을 받은 차량의 한 캠페인 업데이트 성공·실패 수. DB 집계 한 행이다.
 *
 * @param edgeId 차량이 실제로 파일을 받은 Edge 의 id. null 불가
 * @param campaignId 캠페인 ID. null 불가
 * @param succeeded 업데이트에 성공한 차량 수
 * @param failed 업데이트에 실패한 차량 수. 사유와 상관없이 모두 더한다
 */
public record EdgeDownloadCount(String edgeId, Long campaignId, long succeeded, long failed) {

    public EdgeDownloadCount {
        Objects.requireNonNull(edgeId, "edgeId");
        Objects.requireNonNull(campaignId, "campaignId");
    }
}
