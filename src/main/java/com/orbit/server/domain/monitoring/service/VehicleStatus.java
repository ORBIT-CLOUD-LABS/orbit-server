package com.orbit.server.domain.monitoring.service;

import java.time.Instant;

/**
 * 대시보드에 보여 주는 차량 한 대의 상태. 스냅샷과 SSE 가 같은 형식을 쓴다. (MONITORING-REQ-004, MONITORING-REQ-001)
 *
 * <p>차량·현재 상태·최근 업데이트 결과를 함께 보여 주므로 DB 에서 바로 이 형태로 읽는다. 다른 컨텍스트의 enum 을 참조하지 않도록 결과와
 * 실패 사유는 문자열로 둔다.
 *
 * @param vehicleId 차량이 보내는 식별자
 * @param model 차종
 * @param currentVersion 차량의 현재 소프트웨어 버전
 * @param lastSeenAt 마지막 체크인 시각. 온라인·오프라인 판단에 쓴다. 체크인한 적 없으면 null
 * @param updatedAt 상태가 마지막으로 바뀐 시각. 스냅샷과 SSE 에 같은 차량이 오면 이 값이 늦은 쪽을 쓴다
 * @param campaignId 최근 업데이트 결과의 캠페인 ID. 결과가 없으면 null
 * @param lastResult 최근 업데이트 결과(SUCCEEDED, FAILED). 결과가 없으면 null
 * @param failureReason 최근 업데이트의 실패 사유. 실패가 아니면 null
 * @param location [Phase2] 마지막 위치. 위치를 받은 적 없으면 null
 * @param assignedEdgeId [Phase2] 매니페스트가 마지막으로 배정한 Edge. 배정한 적 없으면 null
 * @param downloadedEdgeId [Phase2] 최근 업데이트 결과에서 차량이 실제로 파일을 받은 Edge. 없으면 null
 */
public record VehicleStatus(
        String vehicleId,
        String model,
        String currentVersion,
        Instant lastSeenAt,
        Instant updatedAt,
        Long campaignId,
        String lastResult,
        String failureReason,
        VehicleLastLocation location,
        String assignedEdgeId,
        String downloadedEdgeId) {

    /**
     * 뷰의 컬럼을 그대로 받는 JPQL 생성자 표현식용 생성자. 위치가 없으면 null 로 둔다.
     *
     * <p>위도·경도·위치 시각은 DB 제약(ck_vehicle_state_location)으로 함께 있거나 함께 없다. 그래서 위도로 위치가 있는지 판단한다.
     */
    public VehicleStatus(
            String vehicleId,
            String model,
            String currentVersion,
            Instant lastSeenAt,
            Instant updatedAt,
            Long campaignId,
            String lastResult,
            String failureReason,
            Double latitude,
            Double longitude,
            Instant locationUpdatedAt,
            String assignedEdgeId,
            String downloadedEdgeId) {
        this(
                vehicleId,
                model,
                currentVersion,
                lastSeenAt,
                updatedAt,
                campaignId,
                lastResult,
                failureReason,
                latitude == null ? null : new VehicleLastLocation(latitude, longitude, locationUpdatedAt),
                assignedEdgeId,
                downloadedEdgeId);
    }
}
