package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.VehicleDetail;
import java.time.Instant;
import java.util.Optional;

/**
 * 대시보드의 차량 상세. 차량 상태에 HW 버전·지역과 [Phase2] 위치·배정 Edge, 최근 결과의 실패 상세·종료 시각을 더한다.
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
public record VehicleDetailResponse(
        String vehicleId,
        String model,
        String hwVersion,
        String region,
        String currentVersion,
        Instant lastSeenAt,
        Instant updatedAt,
        VehicleLocationResponse location,
        String assignedEdgeId,
        VehicleLastUpdateResponse lastUpdate) {

    /**
     * @param detail 조회한 차량 상세
     * @return 같은 값을 담은 응답
     */
    public static VehicleDetailResponse from(VehicleDetail detail) {
        return new VehicleDetailResponse(
                detail.vehicleId(),
                detail.model(),
                detail.hwVersion(),
                detail.region(),
                detail.currentVersion(),
                detail.lastSeenAt(),
                detail.updatedAt(),
                Optional.ofNullable(detail.location())
                        .map(VehicleLocationResponse::from)
                        .orElse(null),
                detail.assignedEdgeId(),
                Optional.ofNullable(detail.lastUpdate())
                        .map(VehicleLastUpdateResponse::from)
                        .orElse(null));
    }
}
