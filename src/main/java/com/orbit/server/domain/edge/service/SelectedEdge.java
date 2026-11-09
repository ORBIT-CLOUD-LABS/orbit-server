package com.orbit.server.domain.edge.service;

import com.orbit.server.domain.edge.entity.Edge;
import java.net.URI;
import java.util.OptionalDouble;

/**
 * 매니페스트에 내려주려고 고른 Edge 하나. 다른 컨텍스트가 {@code edge} 의 entity 를 참조하지 않도록 필요한 값만 담는다.
 *
 * @param edgeId Edge id
 * @param baseUrl 차량이 파일을 받는 주소. 다운로드 URL 은 이 주소 뒤에 Origin 경로를 붙여 만든다
 * @param distanceKm 차량 위치에서 이 Edge 까지의 거리(km). 차량 위치를 몰라 거리로 고르지 않았으면 빈 값
 */
public record SelectedEdge(String edgeId, URI baseUrl, OptionalDouble distanceKm) {

    static SelectedEdge nearest(Edge edge, double distanceKm) {
        return new SelectedEdge(edge.id(), edge.baseUrl(), OptionalDouble.of(distanceKm));
    }

    static SelectedEdge withoutDistance(Edge edge) {
        return new SelectedEdge(edge.id(), edge.baseUrl(), OptionalDouble.empty());
    }
}
