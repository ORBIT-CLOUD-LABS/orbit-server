package com.orbit.server.domain.edge.service;

import com.orbit.server.domain.edge.entity.Coordinates;
import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.exception.EdgeErrorCode;
import com.orbit.server.global.error.BusinessException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 매니페스트에 내려줄 Edge 를 고른다. 정상인 Edge 중 차량과 가까운 순서로 정렬해 첫 번째를 다운로드 Edge 로, 다음 Edge 들을 예비 Edge 로
 * 둔다.
 *
 * <p>Edge 목록과 헬스체크 결과는 모두 Pod 메모리에 있으므로 DB 를 읽지 않는다.
 */
@Component
public class EdgeSelector {

    private final EdgeHealthBoard healthBoard;
    private final EdgeCatalog catalog;
    private final EdgeSelectionProperties properties;

    EdgeSelector(EdgeHealthBoard healthBoard, EdgeCatalog catalog, EdgeSelectionProperties properties) {
        this.healthBoard = healthBoard;
        this.catalog = catalog;
        this.properties = properties;
    }

    /**
     * 차량의 마지막 위치로 Edge 를 고른다. 위치가 유효 시간 안이면 가까운 순서로, 더 오래됐으면 {@link #selectDefault()} 와 같이 고른다.
     *
     * @param location 차량의 마지막 위치와 받은 뒤 지난 시간
     * @return 다운로드 Edge 와 예비 Edge 들
     * @throws BusinessException 정상인 Edge 가 하나도 없으면 NO_AVAILABLE_EDGE
     */
    // 차량이 움직이므로 오래된 위치로 고르면 가까운 Edge 가 아닐 수 있다
    public EdgeSelection select(ObservedLocation location) {
        if (location.age().compareTo(properties.locationMaxAge()) > 0) {
            return selectDefault();
        }
        return selectNearest(new Coordinates(location.latitude(), location.longitude()));
    }

    /**
     * 차량 위치를 모르거나 너무 오래됐을 때 쓴다. 기본 Edge 가 정상이면 첫 번째로 두고, 나머지는 설정 순서대로 둔다.
     *
     * <p>기본 Edge 가 비정상이면 정상 Edge 만 후보라는 규칙이 우선이므로 기본 Edge 를 빼고 설정 순서를 따른다.
     *
     * @return 기본 Edge(또는 설정 순서 첫 정상 Edge)와 예비 Edge 들. 거리는 모두 빈 값
     * @throws BusinessException 정상인 Edge 가 하나도 없으면 NO_AVAILABLE_EDGE
     */
    public EdgeSelection selectDefault() {
        List<Edge> healthy = requireHealthyEdges();
        List<Edge> ordered = new ArrayList<>(healthy);
        Edge defaultEdge = catalog.edges().defaultEdge();
        if (ordered.remove(defaultEdge)) {
            ordered.addFirst(defaultEdge);
        }
        return toSelection(ordered.stream().map(SelectedEdge::withoutDistance).toList());
    }

    // 거리가 같으면 설정 순서를 따른다(안정 정렬). Edge 는 몇 개뿐이라 정렬과 응답용으로 거리를 두 번 계산해도 비용이 없다
    private EdgeSelection selectNearest(Coordinates vehicle) {
        List<SelectedEdge> candidates = requireHealthyEdges().stream()
                .sorted(Comparator.comparingDouble(edge -> vehicle.distanceKmTo(edge.coordinates())))
                .map(edge -> SelectedEdge.nearest(edge, vehicle.distanceKmTo(edge.coordinates())))
                .toList();
        return toSelection(candidates);
    }

    private List<Edge> requireHealthyEdges() {
        List<Edge> healthy = healthBoard.healthyEdges();
        if (healthy.isEmpty()) {
            throw new BusinessException(EdgeErrorCode.NO_AVAILABLE_EDGE);
        }
        return healthy;
    }

    // 첫 번째가 다운로드 Edge, 그 다음부터 fallback 개수만큼이 예비 Edge 다
    private EdgeSelection toSelection(List<SelectedEdge> ordered) {
        int fallbackEnd = Math.min(ordered.size(), 1 + properties.fallbackCount());
        return new EdgeSelection(ordered.getFirst(), ordered.subList(1, fallbackEnd));
    }
}
