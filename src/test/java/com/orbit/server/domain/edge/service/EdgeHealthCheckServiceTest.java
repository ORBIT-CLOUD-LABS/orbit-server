package com.orbit.server.domain.edge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.edge.entity.Coordinates;
import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.entity.Edges;
import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EdgeHealthCheckServiceTest {

    private static final int THRESHOLD = 3;

    private static final Edge SEOUL =
            new Edge("edge-seoul", "서울", URI.create("http://cdn.test:30081"), new Coordinates(37.5665, 126.9780));
    private static final Edge BUSAN =
            new Edge("edge-busan", "부산", URI.create("http://cdn.test:30083"), new Coordinates(35.1796, 129.0756));

    private final EdgeCatalog catalog = () -> new Edges(List.of(SEOUL, BUSAN), "edge-seoul");
    private final Set<Edge> downEdges = new HashSet<>();
    private final EdgeHealthBoard board =
            new EdgeHealthBoard(catalog, new EdgeHealthCheckProperties(Duration.ofSeconds(10), THRESHOLD));
    private final EdgeHealthCheckService service =
            new EdgeHealthCheckService(catalog, edge -> !downEdges.contains(edge), board);

    @Test
    @DisplayName("모든 Edge 가 응답하면 모두 정상 Edge 로 남긴다")
    void keepsRespondingEdges() {
        checkTimes(THRESHOLD);

        assertThat(board.healthyEdges()).containsExactly(SEOUL, BUSAN);
    }

    @Test
    @DisplayName("기준 횟수만큼 연속으로 응답하지 않은 Edge 만 정상 Edge 에서 뺀다")
    void excludesOnlyFailingEdge() {
        downEdges.add(BUSAN);

        checkTimes(THRESHOLD - 1);
        assertThat(board.healthyEdges()).containsExactly(SEOUL, BUSAN);

        checkTimes(1);
        assertThat(board.healthyEdges()).containsExactly(SEOUL);
    }

    @Test
    @DisplayName("빠진 Edge 가 다시 응답하면 다음 확인에서 되돌린다")
    void restoresRecoveredEdge() {
        downEdges.add(BUSAN);
        checkTimes(THRESHOLD);

        downEdges.remove(BUSAN);
        checkTimes(1);

        assertThat(board.healthyEdges()).containsExactly(SEOUL, BUSAN);
    }

    private void checkTimes(int times) {
        for (int i = 0; i < times; i++) {
            service.checkAll();
        }
    }
}
