package com.orbit.server.domain.monitoring.service.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VehicleStatusTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    @DisplayName("MONITORING-REQ-004: 뷰의 위치 컬럼이 모두 있으면 위치를 담는다")
    void holdsLocationFromViewColumns() {
        VehicleStatus status = fromViewColumns(37.5665, 126.978, UPDATED_AT);

        assertThat(status.location()).isEqualTo(new VehicleLastLocation(37.5665, 126.978, UPDATED_AT));
    }

    // 제약이 어긋난 행 하나가 차량 전체를 읽는 스냅샷·SSE 조회를 실패시키면 안 된다
    @Test
    @DisplayName("MONITORING-REQ-004: 뷰에 위도만 있고 경도가 없어도 예외 없이 위치를 비운다")
    void holdsNoLocationWithoutLongitude() {
        VehicleStatus status = fromViewColumns(37.5665, null, UPDATED_AT);

        assertThat(status.location()).isNull();
    }

    private VehicleStatus fromViewColumns(Double latitude, Double longitude, Instant locationUpdatedAt) {
        return new VehicleStatus(
                "vehicle-001",
                "model-a",
                "1.0.0",
                UPDATED_AT,
                UPDATED_AT,
                null,
                null,
                null,
                latitude,
                longitude,
                locationUpdatedAt,
                "edge-seoul",
                null);
    }
}
