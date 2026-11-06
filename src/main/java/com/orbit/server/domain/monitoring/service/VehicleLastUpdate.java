package com.orbit.server.domain.monitoring.service;

import java.time.Instant;

/**
 * 차량이 마지막으로 보고한 업데이트 결과. 보고 시각(reported_at)이 늦은 것이고, 같으면 나중에 기록한 것이다.
 *
 * <p>다른 컨텍스트의 enum 을 참조하지 않도록 결과와 실패 사유는 문자열로 둔다.
 *
 * @param campaignId 결과를 보고한 캠페인 ID
 * @param result 업데이트 결과(SUCCEEDED, FAILED)
 * @param failureReason 실패 사유. 실패가 아니면 null
 * @param failureDetail 차량이 보낸 실패 상세. 보내지 않았으면 null
 * @param edgeId [Phase2] 차량이 실제로 파일을 받은 Edge. 다운로드 전에 실패했거나 Phase1 결과면 null
 * @param finishedAt 차량이 업데이트를 끝낸 시각
 */
public record VehicleLastUpdate(
        Long campaignId,
        String result,
        String failureReason,
        String failureDetail,
        String edgeId,
        Instant finishedAt) {}
