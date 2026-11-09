package com.orbit.server.domain.update.service;

import com.orbit.server.domain.edge.service.EdgeSelection;
import com.orbit.server.domain.edge.service.EdgeSelector;
import com.orbit.server.domain.edge.service.ObservedLocation;
import com.orbit.server.domain.edge.service.SelectedEdge;
import com.orbit.server.domain.update.service.EdgeAssignment.FallbackUrl;
import com.orbit.server.domain.update.service.ManifestVehicleState.LastLocation;
import com.orbit.server.global.error.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 매니페스트를 요청한 차량에 Edge 를 배정한다. 차량의 마지막 위치로 Edge 를 고르고, 다운로드 URL 을 만들고, 고른 Edge 를 차량 상태에
 * 기록한다.
 *
 * <p>Edge 목록과 선택 규칙(위치 유효 시간 포함)은 {@code edge} 컨텍스트의 일이므로 그 서비스로만 접근한다.
 */
@Component
@RequiredArgsConstructor
public class EdgeAssigner {

    private final VehicleStateService vehicleStateService;
    private final EdgeSelector edgeSelector;
    private final DownloadUrlIssuer downloadUrlIssuer;

    /**
     * 위치가 있으면 그 위치로, 없으면 기본 Edge 우선으로 고른다. 고른 Edge 는 차량 상태에 기록한다.
     *
     * @param vehicle 매니페스트를 요청한 차량의 상태
     * @param path 받을 파일의 Origin 경로
     * @return 배정한 Edge 와 다운로드·예비 URL
     * @throws BusinessException 정상인 Edge 가 하나도 없으면 NO_AVAILABLE_EDGE
     * @throws IllegalStateException 차량의 상태가 없는 경우
     */
    public EdgeAssignment assign(ManifestVehicleState vehicle, String path) {
        EdgeSelection selection = vehicle.lastLocation()
                .map(location -> edgeSelector.select(observed(location)))
                .orElseGet(edgeSelector::selectDefault);
        vehicleStateService.assignEdge(vehicle.vehicleId(), selection.primary().edgeId());
        return new EdgeAssignment(
                selection.primary(), urlOf(selection.primary(), path), fallbackUrlsOf(selection, path));
    }

    private static ObservedLocation observed(LastLocation last) {
        return new ObservedLocation(last.location().latitude(), last.location().longitude(), last.age());
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
