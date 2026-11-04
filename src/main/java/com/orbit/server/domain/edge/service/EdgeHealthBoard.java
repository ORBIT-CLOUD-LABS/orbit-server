package com.orbit.server.domain.edge.service;

import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.entity.EdgeHealth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Edge 별 헬스체크 결과를 Pod 메모리에 두고, 매니페스트의 Edge 선택 후보가 될 정상 Edge 를 알려 준다.
 *
 * <p>결과를 Pod 사이에 맞추려면 공유 저장소나 리더 선출이 필요해 Phase2 에서는 Pod 마다 따로 둔다(#84). 그래서 장애 직후에는 Pod 마다 판정이
 * 잠깐 다를 수 있고, 차량은 fallbackUrls 로 버틴다.
 */
@Slf4j
@Component
public class EdgeHealthBoard {

    private final EdgeCatalog catalog;
    private final EdgeHealthCheckProperties properties;

    // 기록은 헬스체크 스레드 하나만 하고 조회는 요청 스레드들이 하므로, 원자적 갱신보다 쓰기 결과가 바로 보이는 것이 필요하다
    private final Map<String, EdgeHealth> healths = new ConcurrentHashMap<>();

    EdgeHealthBoard(EdgeCatalog catalog, EdgeHealthCheckProperties properties) {
        this.catalog = catalog;
        this.properties = properties;
    }

    /**
     * @return 정상인 Edge 목록. 설정 순서를 따르고, 모두 비정상이면 빈 목록
     */
    public List<Edge> healthyEdges() {
        return catalog.edges().all().stream().filter(this::isHealthy).toList();
    }

    /**
     * @param edge 확인할 Edge
     * @return 연속 실패가 기준 횟수보다 적으면 true. 아직 확인하지 않은 Edge 도 true
     */
    public boolean isHealthy(Edge edge) {
        return healthOf(edge).isHealthy(properties.failureThreshold());
    }

    void recordSuccess(Edge edge) {
        record(edge, EdgeHealth::success);
    }

    void recordFailure(Edge edge) {
        record(edge, EdgeHealth::failure);
    }

    private void record(Edge edge, UnaryOperator<EdgeHealth> next) {
        boolean wasHealthy = isHealthy(edge);
        healths.put(edge.id(), next.apply(healthOf(edge)));
        logChange(edge, wasHealthy);
    }

    private EdgeHealth healthOf(Edge edge) {
        return healths.getOrDefault(edge.id(), EdgeHealth.initial());
    }

    // 매 확인마다 남기면 정상 상태에서도 로그가 쌓이므로 판정이 바뀔 때만 남긴다
    private void logChange(Edge edge, boolean wasHealthy) {
        boolean healthy = isHealthy(edge);
        if (wasHealthy && !healthy) {
            log.warn("Edge 가 비정상으로 바뀌었습니다: edgeId={}", edge.id());
            return;
        }
        if (!wasHealthy && healthy) {
            log.info("Edge 가 복구되었습니다: edgeId={}", edge.id());
        }
    }
}
