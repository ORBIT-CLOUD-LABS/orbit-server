package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 차량 상세는 보조 API(제안)라 요구사항 ID 가 없다
class VehicleDetailTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    @DisplayName("차량 상세: 뷰의 위치 컬럼이 모두 있으면 위치를 담는다")
    void holdsLocationFromViewColumns() {
        VehicleDetail detail = fromViewColumns(37.5665, 126.978, UPDATED_AT);

        assertThat(detail.location()).isEqualTo(new VehicleLastLocation(37.5665, 126.978, UPDATED_AT));
    }

    @Test
    @DisplayName("차량 상세: 뷰에 위도만 있고 경도가 없어도 예외 없이 위치를 비운다")
    void holdsNoLocationWithoutLongitude() {
        VehicleDetail detail = fromViewColumns(37.5665, null, UPDATED_AT);

        assertThat(detail.location()).isNull();
    }

    private VehicleDetail fromViewColumns(Double latitude, Double longitude, Instant locationUpdatedAt) {
        return new VehicleDetail(
                "vehicle-001",
                "model-a",
                "hw-1",
                "KR",
                "1.0.0",
                UPDATED_AT,
                UPDATED_AT,
                latitude,
                longitude,
                locationUpdatedAt,
                "edge-seoul",
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
