package com.orbit.server.domain.update.service;

import com.orbit.server.domain.edge.service.EdgeSelection;
import com.orbit.server.domain.edge.service.EdgeSelectionProperties;
import com.orbit.server.domain.edge.service.EdgeSelector;
import com.orbit.server.domain.edge.service.SelectedEdge;
import com.orbit.server.domain.update.service.EdgeAssignment.FallbackUrl;
import com.orbit.server.global.error.BusinessException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매니페스트를 요청한 차량에 Edge 를 배정한다. 차량의 마지막 위치로 가까운 Edge 를 고르고, 다운로드 URL 을 만들고, 고른 Edge 를 차량 상태에
 * 기록한다.
 *
 * <p>Edge 목록과 선택 규칙은 {@code edge} 컨텍스트의 일이므로 그 서비스로만 접근한다.
 */
@Component
@RequiredArgsConstructor
public class EdgeAssigner {

    private final VehicleStateService vehicleStateService;
    private final EdgeSelector edgeSelector;
    private final EdgeSelectionProperties selectionProperties;
    private final DownloadUrlIssuer downloadUrlIssuer;

    /**
     * 위치가 유효하면 가까운 순서로, 없거나 오래됐으면 기본 Edge 우선으로 고른다. 고른 Edge 는 차량 상태에 기록한다.
     *
     * @param vehicleId 차량 ID
     * @param path 받을 파일의 Origin 경로
     * @param now 위치가 유효한지 판단할 기준 시각
     * @return 배정한 Edge 와 다운로드·예비 URL
     * @throws BusinessException 정상인 Edge 가 하나도 없으면 NO_AVAILABLE_EDGE
     * @throws IllegalStateException 차량의 상태가 없는 경우
     */
    // 위치 조회와 배정 기록을 한 트랜잭션으로 묶어 같은 상태 행을 두 번 읽지 않게 한다
    @Transactional
    public EdgeAssignment assign(Long vehicleId, String path, Instant now) {
        EdgeSelection selection = select(vehicleId, now);
        vehicleStateService.assignEdge(vehicleId, selection.primary().edgeId());
        return new EdgeAssignment(
                selection.primary(), urlOf(selection.primary(), path), fallbackUrlsOf(selection, path));
    }

    private EdgeSelection select(Long vehicleId, Instant now) {
        return vehicleStateService
                .findFreshLocation(vehicleId, now, selectionProperties.locationMaxAge())
                .map(location -> edgeSelector.selectNearest(location.latitude(), location.longitude()))
                .orElseGet(edgeSelector::selectDefault);
    }

    private List<FallbackUrl> fallbackUrlsOf(EdgeSelection selection, String path) {
        return selection.fallbacks().stream()
                .map(edge -> new FallbackUrl(edge.edgeId(), urlOf(edge, path)))
                .toList();
    }

    private String urlOf(SelectedEdge edge, String path) {
        return downloadUrlIssuer.issue(edge.baseUrl(), path);
    }
}
