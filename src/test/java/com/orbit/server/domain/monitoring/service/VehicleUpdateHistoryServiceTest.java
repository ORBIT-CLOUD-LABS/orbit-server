package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.orbit.server.domain.monitoring.repository.VehicleUpdateHistoryRepository;
import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.error.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

// 차량 업데이트 이력은 보조 API(제안)라 요구사항 ID 가 없다
class VehicleUpdateHistoryServiceTest {

    private static final Long VEHICLE_KEY = 7L;
    private static final VehicleUpdateAttempt ATTEMPT = new VehicleUpdateAttempt(
            1L, "1.0.1", "FAILED", "HASH_MISMATCH", null, null, Instant.parse("2026-10-01T12:00:00Z"));

    private final VehicleUpdateHistoryRepository repository = mock(VehicleUpdateHistoryRepository.class);
    private final VehicleUpdateHistoryService service = new VehicleUpdateHistoryService(repository);

    @Test
    @DisplayName("차량 업데이트 이력: 차량의 최근 업데이트 시도 50건을 조회한다")
    void getsRecentAttempts() {
        given(repository.findVehicleKey("vehicle-001")).willReturn(Optional.of(VEHICLE_KEY));
        given(repository.findRecentAttempts(VEHICLE_KEY, Limit.of(50))).willReturn(List.of(ATTEMPT));

        assertThat(service.getRecentAttempts("vehicle-001")).containsExactly(ATTEMPT);
    }

    @Test
    @DisplayName("차량 업데이트 이력: 차량이 없으면 VEHICLE_NOT_FOUND 로 거부한다")
    void rejectsMissingVehicle() {
        given(repository.findVehicleKey("vehicle-999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRecentAttempts("vehicle-999"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", VehicleErrorCode.VEHICLE_NOT_FOUND);
    }
}
