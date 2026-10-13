package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VehicleStatusQueryServiceTest {

    private static final Instant AS_OF = Instant.parse("2026-10-01T12:00:00Z");
    private static final VehicleStatus VEHICLE =
            new VehicleStatus("vehicle-001", "model-a", "1.0.0", AS_OF, AS_OF, 1L, "FAILED", "HASH_MISMATCH");

    @Test
    @DisplayName("MONITORING-REQ-004: 기준 시각과 전체 차량 상태를 스냅샷으로 조회한다")
    void getsSnapshot() {
        VehicleStatusSnapshot snapshot = new VehicleStatusSnapshot(AS_OF, List.of(VEHICLE));
        VehicleStatusQueryService service = new VehicleStatusQueryService(() -> snapshot);

        assertThat(service.getSnapshot()).isEqualTo(snapshot);
    }
}
