package com.orbit.server.domain.monitoring.service;

import com.orbit.server.domain.edge.service.EdgeHealthBoard;
import com.orbit.server.domain.monitoring.repository.EdgeUsageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 대시보드에 보여 줄 CDN Edge 상태를 조회한다.
 *
 * <p>Edge 목록과 정상 여부는 다른 컨텍스트인 edge 에 {@link EdgeHealthBoard} 로 묻고, 사용 현황은 테이블에서 바로 집계해 읽는다. Edge 는
 * DB 가 아니라 배포 설정에만 있어 조인할 수 없으므로 여기서 합친다.
 *
 * <p>배정 차량 수와 다운로드 수는 서로 맞춰 볼 값이 아니라 트랜잭션으로 묶지 않는다.
 */
@Service
@RequiredArgsConstructor
public class CdnEdgeStatusService {

    private final EdgeHealthBoard edgeHealthBoard;
    private final EdgeUsageRepository edgeUsageRepository;

    /**
     * 설정의 모든 Edge 의 위치·정상 여부·배정 차량 수·캠페인별 다운로드 성공·실패 수를 조회한다.
     *
     * <p>설정의 Edge 를 기준으로 하므로, 설정에서 빠진 Edge 의 과거 기록은 담지 않는다.
     *
     * @return 설정 순서대로의 Edge 상태. 배정 차량이 없으면 0, 다운로드 기록이 없으면 빈 목록
     */
    public List<CdnEdgeStatus> getStatuses() {
        EdgeVehicleCounts vehicles = new EdgeVehicleCounts(edgeUsageRepository.findAssignedVehicleCounts());
        EdgeDownloadCounts downloads = new EdgeDownloadCounts(edgeUsageRepository.findDownloadCounts());
        return edgeHealthBoard.statuses().stream()
                .map(edge -> CdnEdgeStatus.of(edge, vehicles, downloads))
                .toList();
    }
}
