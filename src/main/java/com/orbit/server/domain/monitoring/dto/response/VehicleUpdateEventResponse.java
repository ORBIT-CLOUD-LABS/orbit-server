package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.VehicleUpdateAttempt;
import java.time.Instant;

/**
 * 차량 업데이트 이력의 업데이트 시도 한 번.
 *
 * @param campaignId 업데이트를 배포한 캠페인 ID
 * @param version 캠페인이 배포한 업데이트 파일의 버전
 * @param result 업데이트 결과(SUCCEEDED, FAILED)
 * @param failureReason 실패 사유. 실패가 아니면 null
 * @param failureDetail 차량이 보낸 실패 상세. 보내지 않았으면 null
 * @param edgeId [Phase2] 차량이 실제로 파일을 받은 Edge. 다운로드 전에 실패했거나 Phase1 결과면 null
 * @param finishedAt 차량이 업데이트를 끝낸 시각
 */
public record VehicleUpdateEventResponse(
        Long campaignId,
        String version,
        String result,
        String failureReason,
        String failureDetail,
        String edgeId,
        Instant finishedAt) {

    /**
     * @param attempt 조회한 업데이트 시도
     * @return 같은 값을 담은 응답
     */
    public static VehicleUpdateEventResponse from(VehicleUpdateAttempt attempt) {
        return new VehicleUpdateEventResponse(
                attempt.campaignId(),
                attempt.version(),
                attempt.result(),
                attempt.failureReason(),
                attempt.failureDetail(),
                attempt.edgeId(),
                attempt.finishedAt());
    }
}
