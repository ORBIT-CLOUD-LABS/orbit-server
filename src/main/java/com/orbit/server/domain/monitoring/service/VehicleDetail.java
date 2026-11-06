package com.orbit.server.domain.monitoring.service;

import java.time.Instant;

/**
 * 대시보드에 보여 주는 차량 한 대의 상세. 차량 정보·현재 상태·[Phase2] 위치와 배정 Edge·최근 업데이트 결과를 함께 담는다.
 *
 * <p>스냅샷·SSE 와 같은 기준으로 최근 결과를 고르도록 vehicle_status 뷰에서 바로 이 형태로 읽는다.
 *
 * @param vehicleId 차량이 보내는 식별자
 * @param model 차종
 * @param hwVersion HW 버전
 * @param region 판매·등록 지역
 * @param currentVersion 차량의 현재 소프트웨어 버전
 * @param lastSeenAt 마지막 체크인 시각. 체크인한 적 없으면 null
 * @param updatedAt 상태가 마지막으로 바뀐 시각
 * @param location [Phase2] 마지막 위치. 위치를 받은 적 없으면 null
 * @param assignedEdgeId [Phase2] 매니페스트가 마지막으로 배정한 Edge. 배정한 적 없으면 null
 * @param lastUpdate 최근 업데이트 결과. 결과가 없으면 null
 */
public record VehicleDetail(
        String vehicleId,
        String model,
        String hwVersion,
        String region,
        String currentVersion,
        Instant lastSeenAt,
        Instant updatedAt,
        VehicleLastLocation location,
        String assignedEdgeId,
        VehicleLastUpdate lastUpdate) {

    /**
     * 뷰의 컬럼을 그대로 받는 JPQL 생성자 표현식용 생성자. 위치와 최근 결과가 없으면 각각 null 로 둔다.
     *
     * <p>위도·경도·위치 시각은 DB 제약(ck_vehicle_state_location)으로 함께 있거나 함께 없고, 결과 컬럼은 최근 결과가 있을 때만
     * 값이 있다. 그래서 위도와 캠페인 ID 로 각각 있는지 판단한다.
     */
    public VehicleDetail(
            String vehicleId,
            String model,
            String hwVersion,
            String region,
            String currentVersion,
            Instant lastSeenAt,
            Instant updatedAt,
            Double latitude,
            Double longitude,
            Instant locationUpdatedAt,
            String assignedEdgeId,
            Long campaignId,
            String result,
            String failureReason,
            String failureDetail,
            String downloadedEdgeId,
            Instant finishedAt) {
        this(
                vehicleId,
                model,
                hwVersion,
                region,
                currentVersion,
                lastSeenAt,
                updatedAt,
                latitude == null ? null : new VehicleLastLocation(latitude, longitude, locationUpdatedAt),
                assignedEdgeId,
                campaignId == null
                        ? null
                        : new VehicleLastUpdate(
                                campaignId, result, failureReason, failureDetail, downloadedEdgeId, finishedAt));
    }
}
