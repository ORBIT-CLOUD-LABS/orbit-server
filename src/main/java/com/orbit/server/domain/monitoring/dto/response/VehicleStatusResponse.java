package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.VehicleStatus;
import java.time.Instant;
import java.util.Optional;

/**
 * 대시보드의 차량 한 대 상태. 스냅샷과 SSE 이벤트가 같은 형식을 쓴다. (MONITORING-REQ-004, MONITORING-REQ-001)
 *
 * @param vehicleId 차량이 보내는 식별자
 * @param model 차종
 * @param currentVersion 차량의 현재 소프트웨어 버전
 * @param lastSeenAt 마지막 체크인 시각. 체크인한 적 없으면 null
 * @param updatedAt 상태가 마지막으로 바뀐 시각. 스냅샷과 SSE 에 같은 차량이 오면 이 값이 늦은 쪽을 쓴다
 * @param campaignId 최근 업데이트 결과의 캠페인 ID. 결과가 없으면 null
 * @param lastResult 최근 업데이트 결과(SUCCEEDED, FAILED). 결과가 없으면 null
 * @param failureReason 최근 업데이트의 실패 사유. 실패가 아니면 null
 * @param location [Phase2] 마지막 위치. 위치를 받은 적 없으면 null
 * @param assignedEdgeId [Phase2] 매니페스트가 마지막으로 배정한 Edge. 배정한 적 없으면 null
 * @param downloadedEdgeId [Phase2] 최근 업데이트 결과에서 차량이 실제로 파일을 받은 Edge. 없으면 null
 */
public record VehicleStatusResponse(
        String vehicleId,
        String model,
        String currentVersion,
        Instant lastSeenAt,
        Instant updatedAt,
        Long campaignId,
        String lastResult,
        String failureReason,
        VehicleLocationResponse location,
        String assignedEdgeId,
        String downloadedEdgeId) {

    /**
     * @param status 조회한 차량 상태
     * @return 같은 값을 담은 응답
     */
    public static VehicleStatusResponse from(VehicleStatus status) {
        return new VehicleStatusResponse(
                status.vehicleId(),
                status.model(),
                status.currentVersion(),
                status.lastSeenAt(),
                status.updatedAt(),
                status.campaignId(),
                status.lastResult(),
                status.failureReason(),
                Optional.ofNullable(status.location())
                        .map(VehicleLocationResponse::from)
                        .orElse(null),
                status.assignedEdgeId(),
                status.downloadedEdgeId());
    }
}
