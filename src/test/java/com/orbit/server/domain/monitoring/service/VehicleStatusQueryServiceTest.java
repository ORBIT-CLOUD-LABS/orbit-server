package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.orbit.server.domain.monitoring.repository.VehicleStatusViewRepository;
import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.error.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
        VehicleStatusViewRepository query = mock(VehicleStatusViewRepository.class);
        given(query.findSnapshot()).willReturn(snapshot);
        VehicleStatusQueryService service = new VehicleStatusQueryService(query);

        assertThat(service.getSnapshot()).isEqualTo(snapshot);
    }

    // 차량 상세는 보조 API(제안)라 요구사항 ID 가 없다
    @Test
    @DisplayName("차량 상세: 차량 한 대의 상세를 조회한다")
    void getsDetail() {
        VehicleDetail detail =
                new VehicleDetail("vehicle-001", "model-a", "hw-1", "KR", "1.0.0", AS_OF, AS_OF, null, null, null);
        VehicleStatusViewRepository query = mock(VehicleStatusViewRepository.class);
        given(query.findDetail("vehicle-001")).willReturn(Optional.of(detail));
        VehicleStatusQueryService service = new VehicleStatusQueryService(query);

        assertThat(service.getDetail("vehicle-001")).isEqualTo(detail);
    }

    @Test
    @DisplayName("차량 상세: 차량이 없으면 VEHICLE_NOT_FOUND 로 거부한다")
    void rejectsMissingVehicle() {
        VehicleStatusViewRepository query = mock(VehicleStatusViewRepository.class);
        given(query.findDetail("vehicle-999")).willReturn(Optional.empty());
        VehicleStatusQueryService service = new VehicleStatusQueryService(query);

        assertThatThrownBy(() -> service.getDetail("vehicle-999"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", VehicleErrorCode.VEHICLE_NOT_FOUND);
    }
}
