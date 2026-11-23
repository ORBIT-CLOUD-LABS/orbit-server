package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.result.CdnEdgeStatus;
import java.util.List;

/**
 * [Phase2] 대시보드 지도에 그릴 CDN Edge 하나의 위치·정상 여부와 사용 현황.
 *
 * <p>차량이 파일을 받는 주소는 내부 주소라 응답에 넣지 않는다.
 *
 * @param edgeId Edge id
 * @param name 대시보드에 보여 줄 이름
 * @param latitude Edge 대표 좌표의 위도
 * @param longitude Edge 대표 좌표의 경도
 * @param healthy 이 요청을 받은 서버 Pod 의 헬스체크 결과. Pod 마다 잠깐 다를 수 있다
 * @param assignedVehicles 매니페스트가 이 Edge 를 마지막으로 배정한 차량 수. 없으면 0
 * @param downloads 이 Edge 에서 파일을 받은 결과가 있는 캠페인별 성공·실패 수. campaignId 오름차순, 없으면 빈 목록
 */
public record CdnEdgeStatusResponse(
        String edgeId,
        String name,
        double latitude,
        double longitude,
        boolean healthy,
        long assignedVehicles,
        List<CdnEdgeDownloadResponse> downloads) {

    /**
     * @param status 조회한 Edge 상태
     * @return 같은 값을 담은 응답
     */
    public static CdnEdgeStatusResponse from(CdnEdgeStatus status) {
        return new CdnEdgeStatusResponse(
                status.edgeId(),
                status.name(),
                status.latitude(),
                status.longitude(),
                status.healthy(),
                status.assignedVehicles(),
                status.downloads().stream().map(CdnEdgeDownloadResponse::from).toList());
    }
}
