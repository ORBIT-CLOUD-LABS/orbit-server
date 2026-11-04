package com.orbit.server.domain.edge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.edge.entity.Coordinates;
import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.entity.Edges;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EdgeHealthMetricsTest {

    private static final int THRESHOLD = 3;

    private static final Edge SEOUL =
            new Edge("edge-seoul", "서울", URI.create("http://cdn.test:30081"), new Coordinates(37.5665, 126.9780));
    private static final Edge BUSAN =
            new Edge("edge-busan", "부산", URI.create("http://cdn.test:30083"), new Coordinates(35.1796, 129.0756));

    private final EdgeCatalog catalog = () -> new Edges(List.of(SEOUL, BUSAN), "edge-seoul");
    private final EdgeHealthBoard board =
            new EdgeHealthBoard(catalog, new EdgeHealthCheckProperties(Duration.ofSeconds(10), THRESHOLD));
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void bindMetrics() {
        new EdgeHealthMetrics(catalog, board).bindTo(registry);
    }

    @Test
    @DisplayName("Edge 마다 edge_id 태그를 단 정상 여부 gauge 를 등록한다")
    void registersGaugePerEdge() {
        assertThat(registry.get(EdgeHealthMetrics.NAME).gauges()).hasSize(2);
        assertThat(healthyValueOf(SEOUL)).isEqualTo(1);
        assertThat(healthyValueOf(BUSAN)).isEqualTo(1);
    }

    @Test
    @DisplayName("비정상으로 바뀐 Edge 는 0, 복구되면 다시 1 이 된다")
    void followsHealthChanges() {
        for (int i = 0; i < THRESHOLD; i++) {
            board.recordFailure(BUSAN);
        }
        assertThat(healthyValueOf(BUSAN)).isZero();
        assertThat(healthyValueOf(SEOUL)).isEqualTo(1);

        board.recordSuccess(BUSAN);
        assertThat(healthyValueOf(BUSAN)).isEqualTo(1);
    }

    private double healthyValueOf(Edge edge) {
        return registry.get(EdgeHealthMetrics.NAME)
                .tag(EdgeHealthMetrics.EDGE_ID_TAG, edge.id())
                .gauge()
                .value();
    }
}
