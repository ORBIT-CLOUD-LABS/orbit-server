package com.orbit.server.domain.monitoring.service;

import com.orbit.server.domain.edge.service.EdgeStatus;
import java.util.List;
import java.util.Objects;

/**
 * 대시보드 지도에 그릴 CDN Edge 하나의 위치·정상 여부와 사용 현황.
 *
 * @param edgeId Edge id. null 불가
 * @param name 대시보드에 보여 줄 이름. null 불가
 * @param latitude Edge 대표 좌표의 위도
 * @param longitude Edge 대표 좌표의 경도
 * @param healthy 이 요청을 받은 Pod 의 헬스체크 기준 정상 여부
 * @param assignedVehicles 매니페스트가 이 Edge 를 마지막으로 배정한 차량 수
 * @param downloads 이 Edge 에서 파일을 받은 결과가 있는 캠페인별 성공·실패 수. campaignId 오름차순, 없으면 빈 목록. null 불가
 */
public record CdnEdgeStatus(
        String edgeId,
        String name,
        double latitude,
        double longitude,
        boolean healthy,
        long assignedVehicles,
        List<EdgeDownloadCount> downloads) {

    public CdnEdgeStatus {
        Objects.requireNonNull(edgeId, "edgeId");
        Objects.requireNonNull(name, "name");
        downloads = List.copyOf(downloads);
    }

    /**
     * @param edge 설정의 Edge 하나의 위치와 정상 여부
     * @param vehicles Edge 별 배정 차량 수
     * @param downloads Edge·캠페인별 성공·실패 수
     * @return 이 Edge 의 위치·정상 여부에 이 Edge 의 집계만 골라 합친 상태
     */
    public static CdnEdgeStatus of(EdgeStatus edge, EdgeVehicleCounts vehicles, EdgeDownloadCounts downloads) {
        return new CdnEdgeStatus(
                edge.edgeId(),
                edge.name(),
                edge.latitude(),
                edge.longitude(),
                edge.healthy(),
                vehicles.of(edge.edgeId()),
                downloads.of(edge.edgeId()));
    }
}
