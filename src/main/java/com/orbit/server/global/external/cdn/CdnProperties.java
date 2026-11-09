package com.orbit.server.global.external.cdn;

import com.orbit.server.domain.edge.entity.Coordinates;
import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.entity.Edges;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 차량이 업데이트 파일을 받는 CDN 설정. 배포 환경마다 주소와 Edge 구성이 달라 배포 설정으로 주입한다.
 *
 * <p>값이 잘못된 채로 뜨면 매니페스트마다 잘못된 URL·Edge 를 내려주게 되므로, 값이 없거나 범위를 벗어나면 앱을 띄우지 않는다. Edge
 * 목록의 비어 있음·id 중복·기본 Edge 존재 여부는 도메인 규칙이라 여기서 보지 않고 {@code Edges} 를 만들 때 검증한다. Edge 를 고르는
 * 규칙의 설정(fallback 개수, 위치 유효 시간)은 같은 접두사지만 도메인 규칙이라 {@code edge} 컨텍스트의 {@code
 * EdgeSelectionProperties} 가 읽는다.
 *
 * @param baseUrl Phase1 고정 다운로드 URL 의 CDN 주소. 환경 변수 {@code CDN_BASE_URL} 로 주입한다. 예: {@code
 *     http://cdn.orbit.internal}
 * @param defaultEdgeId 차량 위치를 모를 때 쓰는 Edge 의 id
 * @param edges Edge 목록. 중첩 리스트라 환경 변수가 아니라 외부 YAML 파일로 넣는다
 */
@Validated
@ConfigurationProperties("orbit.cdn")
record CdnProperties(
        @NotNull URI baseUrl,
        String defaultEdgeId,
        // 목록이 빠지면 null 이 아니라 빈 목록으로 받아, 비어 있다는 같은 이유로 Edges 가 거부하게 한다
        @DefaultValue List<@Valid EdgeProperties> edges) {

    /**
     * 설정한 Edge 목록을 도메인의 Edge 목록으로 바꾼다.
     *
     * @return 검증을 마친 Edge 목록
     * @throws InvalidValueException 목록이 비었거나, id 가 겹치거나, 기본 Edge 가 없거나, Edge 값이 잘못된 경우
     */
    Edges toEdges() {
        List<Edge> configured = edges.stream().map(EdgeProperties::toEdge).toList();
        return new Edges(configured, defaultEdgeId);
    }

    /**
     * Edge 하나의 설정. id·이름·주소 검증은 {@code Edge} 가 맡는다.
     *
     * <p>좌표는 빠지면 0 으로 바인딩되어 적도 위의 엉뚱한 Edge 가 되므로, 원시 타입이 아니라 래퍼 타입으로 받아 빠졌는지 확인한다.
     *
     * @param id Edge id
     * @param name 대시보드에 보여 줄 이름
     * @param baseUrl 차량이 파일을 받는 주소
     * @param latitude 대표 좌표의 위도
     * @param longitude 대표 좌표의 경도
     */
    record EdgeProperties(
            String id,
            String name,
            URI baseUrl,
            @NotNull Double latitude,
            @NotNull Double longitude) {

        private Edge toEdge() {
            return new Edge(id, name, baseUrl, new Coordinates(latitude, longitude));
        }
    }
}
