package com.orbit.server.domain.edge.service;

import com.orbit.server.domain.edge.domain.Coordinates;
import com.orbit.server.domain.edge.domain.Edge;

/**
 * 대시보드에 보여 줄 Edge 하나의 위치와 정상 여부. 다른 컨텍스트가 {@code edge} 의 domain 을 참조하지 않도록 필요한 값만 담는다.
 *
 * <p>차량이 파일을 받는 주소는 내부 주소라 대시보드에 내보내지 않으므로 담지 않는다.
 *
 * @param edgeId Edge id
 * @param name 대시보드에 보여 줄 이름
 * @param latitude Edge 대표 좌표의 위도
 * @param longitude Edge 대표 좌표의 경도
 * @param healthy 이 Pod 의 헬스체크 기준 정상 여부. Pod 마다 판정이 잠깐 다를 수 있다
 */
public record EdgeStatus(String edgeId, String name, double latitude, double longitude, boolean healthy) {

    static EdgeStatus of(Edge edge, boolean healthy) {
        Coordinates coordinates = edge.coordinates();
        return new EdgeStatus(edge.id(), edge.name(), coordinates.latitude(), coordinates.longitude(), healthy);
    }
}
