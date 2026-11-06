package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.VehicleLastUpdate;
import java.time.Instant;

/**
 * 차량 상세의 최근 업데이트 결과.
 *
 * @param campaignId 결과를 보고한 캠페인 ID
 * @param result 업데이트 결과(SUCCEEDED, FAILED)
 * @param failureReason 실패 사유. 실패가 아니면 null
 * @param failureDetail 차량이 보낸 실패 상세. 보내지 않았으면 null
 * @param edgeId [Phase2] 차량이 실제로 파일을 받은 Edge. 다운로드 전에 실패했거나 Phase1 결과면 null
 * @param finishedAt 차량이 업데이트를 끝낸 시각
 */
public record VehicleLastUpdateResponse(
        Long campaignId, String result, String failureReason, String failureDetail, String edgeId, Instant finishedAt) {

    /**
     * @param lastUpdate 조회한 최근 업데이트 결과
     * @return 같은 값을 담은 응답
     */
    public static VehicleLastUpdateResponse from(VehicleLastUpdate lastUpdate) {
        return new VehicleLastUpdateResponse(
                lastUpdate.campaignId(),
                lastUpdate.result(),
                lastUpdate.failureReason(),
                lastUpdate.failureDetail(),
                lastUpdate.edgeId(),
                lastUpdate.finishedAt());
    }
}
