package com.orbit.server.domain.edge.service;

import com.orbit.server.domain.edge.entity.Edge;
import org.springframework.stereotype.Service;

/**
 * 설정된 모든 Edge 의 상태를 한 번 확인해 결과를 기록한다. 주기 실행은 {@link EdgeHealthCheckSchedule} 이 맡는다.
 *
 * <p>Edge Deployment 를 0 으로 줄이는 것처럼 운영 중에 Edge 를 빼는 일은 설정 변경 없이 이 확인이 처리한다.
 */
@Service
class EdgeHealthCheckService {

    private final EdgeCatalog catalog;
    private final EdgeHealthProbe probe;
    private final EdgeHealthBoard board;

    EdgeHealthCheckService(EdgeCatalog catalog, EdgeHealthProbe probe, EdgeHealthBoard board) {
        this.catalog = catalog;
        this.probe = probe;
        this.board = board;
    }

    void checkAll() {
        catalog.edges().all().forEach(this::check);
    }

    private void check(Edge edge) {
        if (probe.isUp(edge)) {
            board.recordSuccess(edge);
            return;
        }
        board.recordFailure(edge);
    }
}
