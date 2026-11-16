package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EdgeVehicleCountsTest {

    private final EdgeVehicleCounts counts = new EdgeVehicleCounts(
            List.of(new EdgeVehicleCount("edge-seoul", 42), new EdgeVehicleCount("edge-busan", 7)));

    @Test
    @DisplayName("Edge 에 배정된 차량 수를 찾아 준다")
    void findsCountOfEdge() {
        assertThat(counts.of("edge-seoul")).isEqualTo(42);
        assertThat(counts.of("edge-busan")).isEqualTo(7);
    }

    @Test
    @DisplayName("집계에 없는 Edge 는 0 으로 답한다")
    void answersZeroForEdgeWithoutVehicles() {
        assertThat(counts.of("edge-daejeon")).isZero();
    }
}
