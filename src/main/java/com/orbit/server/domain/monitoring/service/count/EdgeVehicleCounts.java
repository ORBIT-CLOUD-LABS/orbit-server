package com.orbit.server.domain.monitoring.service.count;

import java.util.List;

/**
 * Edge 별 배정 차량 수. Edge 하나의 배정 차량 수를 찾아 준다.
 *
 * <p>집계에는 배정된 차량이 있는 Edge 만 나오므로, 대시보드가 Edge 마다 값이 있는지 확인하지 않도록 집계에 없는 Edge 는 0 으로 답한다.
 */
public class EdgeVehicleCounts {

    private final List<EdgeVehicleCount> counts;

    /**
     * @param counts Edge 별 배정 차량 수. 없으면 빈 목록
     */
    public EdgeVehicleCounts(List<EdgeVehicleCount> counts) {
        this.counts = List.copyOf(counts);
    }

    /**
     * @param edgeId 찾을 Edge 의 id
     * @return 이 Edge 에 배정된 차량 수. 배정된 차량이 없으면 0
     */
    public long of(String edgeId) {
        return counts.stream()
                .filter(count -> count.edgeId().equals(edgeId))
                .mapToLong(EdgeVehicleCount::vehicles)
                .sum();
    }
}
