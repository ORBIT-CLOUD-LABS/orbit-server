package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 마지막 위치는 대시보드 표시용 값이라 요구사항 ID 가 없다
class VehicleLastLocationTest {

    private static final Instant LOCATED_AT = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    @DisplayName("마지막 위치: 위도·경도·위치 시각이 모두 있으면 위치를 만든다")
    void createsLocationWithAllValues() {
        assertThat(VehicleLastLocation.of(37.5665, 126.978, LOCATED_AT))
                .contains(new VehicleLastLocation(37.5665, 126.978, LOCATED_AT));
    }

    @Test
    @DisplayName("마지막 위치: 위치를 받은 적 없어 세 값이 모두 없으면 위치가 없다")
    void returnsEmptyWithoutAnyValue() {
        assertThat(VehicleLastLocation.of(null, null, null)).isEmpty();
    }

    @Test
    @DisplayName("마지막 위치: 위도만 없으면 예외 없이 위치가 없다")
    void returnsEmptyWithoutLatitude() {
        assertThat(VehicleLastLocation.of(null, 126.978, LOCATED_AT)).isEmpty();
    }

    @Test
    @DisplayName("마지막 위치: 경도만 없으면 예외 없이 위치가 없다")
    void returnsEmptyWithoutLongitude() {
        assertThat(VehicleLastLocation.of(37.5665, null, LOCATED_AT)).isEmpty();
    }

    @Test
    @DisplayName("마지막 위치: 위치 시각만 없으면 위치가 없다")
    void returnsEmptyWithoutUpdatedAt() {
        assertThat(VehicleLastLocation.of(37.5665, 126.978, null)).isEmpty();
    }
}
