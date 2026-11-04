package com.orbit.server.domain.edge.entity;

import com.orbit.server.global.error.InvalidValueException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 배포 설정으로 정한 CDN Edge 목록과 위치를 모를 때 쓰는 기본 Edge.
 *
 * <p>Edge 목록은 서버가 뜰 때 한 번만 읽으므로, 잘못된 목록으로 뜨면 재배포 전까지 매니페스트가 계속 잘못된 Edge 를 안내한다. 그래서
 * 생성 시점에 막아 기동을 실패시킨다.
 */
public final class Edges {

    private final List<Edge> edges;
    private final Edge defaultEdge;

    /**
     * @param edges Edge 목록. 비어 있을 수 없고 id 가 겹치면 안 된다
     * @param defaultEdgeId 위치를 모를 때 쓸 Edge 의 id. 목록에 있어야 한다
     * @throws InvalidValueException 목록이 없거나 비어 있거나, id 가 겹치거나, 기본 Edge 가 목록에 없는 경우
     */
    public Edges(List<Edge> edges, String defaultEdgeId) {
        requireNotEmpty(edges);
        requireUniqueIds(edges);
        this.edges = List.copyOf(edges);
        this.defaultEdge = findDefault(this.edges, defaultEdgeId);
    }

    /**
     * @return 설정한 순서대로의 Edge 목록. 수정할 수 없다
     */
    public List<Edge> all() {
        return edges;
    }

    /**
     * @return 차량 위치를 모를 때 쓰는 Edge
     */
    public Edge defaultEdge() {
        return defaultEdge;
    }

    private static void requireNotEmpty(List<Edge> edges) {
        if (edges == null || edges.isEmpty()) {
            throw new InvalidValueException("Edge 목록은 비어 있을 수 없습니다.");
        }
    }

    private static void requireUniqueIds(List<Edge> edges) {
        Set<String> ids = new HashSet<>();
        for (Edge edge : edges) {
            requireFirstSeen(ids, edge.id());
        }
    }

    private static void requireFirstSeen(Set<String> ids, String id) {
        if (!ids.add(id)) {
            throw new InvalidValueException("Edge id 가 중복됩니다: " + id);
        }
    }

    private static Edge findDefault(List<Edge> edges, String defaultEdgeId) {
        return edges.stream()
                .filter(edge -> edge.id().equals(defaultEdgeId))
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("기본 Edge 가 목록에 없습니다: " + defaultEdgeId));
    }
}
