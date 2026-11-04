package com.orbit.server.domain.edge.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EdgesTest {

    private static final Edge SEOUL =
            new Edge("edge-seoul", "서울", URI.create("http://cdn.test:30081"), new Coordinates(37.5665, 126.9780));
    private static final Edge BUSAN =
            new Edge("edge-busan", "부산", URI.create("http://cdn.test:30083"), new Coordinates(35.1796, 129.0756));

    @Test
    @DisplayName("설정한 순서대로 Edge 목록을 돌려준다")
    void returnsEdgesInOrder() {
        Edges edges = new Edges(List.of(SEOUL, BUSAN), "edge-seoul");

        assertThat(edges.all()).containsExactly(SEOUL, BUSAN);
    }

    @Test
    @DisplayName("default-edge-id 에 해당하는 Edge 를 기본 Edge 로 돌려준다")
    void returnsDefaultEdge() {
        Edges edges = new Edges(List.of(SEOUL, BUSAN), "edge-busan");

        assertThat(edges.defaultEdge()).isEqualTo(BUSAN);
    }

    @Test
    @DisplayName("만든 뒤 원본 목록을 바꿔도 Edge 목록은 바뀌지 않는다")
    void copiesSourceList() {
        List<Edge> source = new ArrayList<>(List.of(SEOUL));
        Edges edges = new Edges(source, "edge-seoul");

        source.add(BUSAN);

        assertThat(edges.all()).containsExactly(SEOUL);
    }

    @Test
    @DisplayName("Edge 목록이 비어 있으면 거부한다")
    void rejectsEmptyEdges() {
        assertThatThrownBy(() -> new Edges(List.of(), "edge-seoul")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("Edge 목록이 없으면 거부한다")
    void rejectsMissingEdges() {
        assertThatThrownBy(() -> new Edges(null, "edge-seoul")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("id 가 같은 Edge 가 있으면 거부한다")
    void rejectsDuplicateId() {
        Edge anotherSeoul =
                new Edge("edge-seoul", "서울2", URI.create("http://cdn.test:30084"), new Coordinates(37.5, 127.0));

        assertThatThrownBy(() -> new Edges(List.of(SEOUL, anotherSeoul), "edge-seoul"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("edge-seoul");
    }

    @Test
    @DisplayName("default-edge-id 가 목록에 없으면 거부한다")
    void rejectsUnknownDefaultEdgeId() {
        assertThatThrownBy(() -> new Edges(List.of(SEOUL), "edge-daejeon"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("edge-daejeon");
    }
}
