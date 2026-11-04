package com.orbit.server.domain.edge.service;

import com.orbit.server.domain.edge.entity.Edge;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/**
 * Edge 별 정상 여부를 {@code orbit_cdn_edge_healthy{edge_id}} 메트릭(정상 1, 비정상 0)으로 내보낸다.
 *
 * <p>로그는 상태가 바뀔 때만 남으므로, 지금 어느 Edge 가 빠져 있는지는 이 메트릭으로 본다. 값은 이 Pod 의 판정이라 Pod 마다 잠깐 다를 수 있다.
 */
@Component
class EdgeHealthMetrics implements MeterBinder {

    static final String NAME = "orbit.cdn.edge.healthy";
    static final String EDGE_ID_TAG = "edge_id";

    private static final double HEALTHY = 1;
    private static final double UNHEALTHY = 0;

    private final EdgeCatalog catalog;
    private final EdgeHealthBoard board;

    EdgeHealthMetrics(EdgeCatalog catalog, EdgeHealthBoard board) {
        this.catalog = catalog;
        this.board = board;
    }

    // Edge 목록은 기동 후 바뀌지 않으므로 등록할 때 Edge 마다 gauge 를 하나씩 만든다
    @Override
    public void bindTo(MeterRegistry registry) {
        catalog.edges().all().forEach(edge -> register(registry, edge));
    }

    private void register(MeterRegistry registry, Edge edge) {
        Gauge.builder(NAME, () -> healthValueOf(edge))
                .description("Edge 헬스체크 판정. 정상 1, 비정상 0")
                .tag(EDGE_ID_TAG, edge.id())
                .register(registry);
    }

    private double healthValueOf(Edge edge) {
        if (board.isHealthy(edge)) {
            return HEALTHY;
        }
        return UNHEALTHY;
    }
}
