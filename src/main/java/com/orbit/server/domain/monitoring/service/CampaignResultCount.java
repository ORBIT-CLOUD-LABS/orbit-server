package com.orbit.server.domain.monitoring.service;

import java.util.Objects;

/**
 * 한 캠페인의 업데이트 결과를 결과·실패 사유별로 묶은 차량 수. DB 집계 한 행이다.
 *
 * <p>update 컨텍스트의 enum 을 참조하지 않도록 결과와 실패 사유는 문자열로 둔다.
 *
 * @param result 업데이트 결과(SUCCEEDED, FAILED). null 불가
 * @param failureReason 실패 사유. 성공이면 null
 * @param count 차량 수
 */
public record CampaignResultCount(String result, String failureReason, long count) {

    public CampaignResultCount {
        Objects.requireNonNull(result, "result");
    }
}
