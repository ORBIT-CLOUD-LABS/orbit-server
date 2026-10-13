package com.orbit.server.domain.monitoring.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.domain.monitoring.service.VehicleStatus;
import com.orbit.server.domain.monitoring.service.VehicleStatusQueryService;
import com.orbit.server.domain.monitoring.service.VehicleStatusSnapshot;
import com.orbit.server.global.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardVehicleControllerTest {

    private static final String URL = "/api/v1/dashboard/vehicles";
    private static final String AS_OF = "2026-10-11T07:00:00.123Z";
    private static final String LAST_SEEN_AT = "2026-10-11T06:59:00.456Z";
    private static final String UPDATED_AT = "2026-10-11T06:59:00.789Z";

    private VehicleStatusQueryService vehicleStatusQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        vehicleStatusQueryService = mock(VehicleStatusQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardVehicleController(vehicleStatusQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("MONITORING-REQ-004: 기준 시각과 업데이트 결과가 있는 차량 상태를 200 으로 응답한다")
    void respondsSnapshotWithUpdateResult() throws Exception {
        VehicleStatus vehicle = new VehicleStatus(
                "vehicle-001",
                "model-a",
                "1.0.0",
                Instant.parse(LAST_SEEN_AT),
                Instant.parse(UPDATED_AT),
                1L,
                "FAILED",
                "HASH_MISMATCH");
        given(vehicleStatusQueryService.getSnapshot())
                .willReturn(new VehicleStatusSnapshot(Instant.parse(AS_OF), List.of(vehicle)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value(AS_OF))
                .andExpect(jsonPath("$.vehicles.length()").value(1))
                .andExpect(jsonPath("$.vehicles[0].vehicleId").value("vehicle-001"))
                .andExpect(jsonPath("$.vehicles[0].model").value("model-a"))
                .andExpect(jsonPath("$.vehicles[0].currentVersion").value("1.0.0"))
                .andExpect(jsonPath("$.vehicles[0].lastSeenAt").value(LAST_SEEN_AT))
                .andExpect(jsonPath("$.vehicles[0].updatedAt").value(UPDATED_AT))
                .andExpect(jsonPath("$.vehicles[0].campaignId").value(1))
                .andExpect(jsonPath("$.vehicles[0].lastResult").value("FAILED"))
                .andExpect(jsonPath("$.vehicles[0].failureReason").value("HASH_MISMATCH"));
    }

    @Test
    @DisplayName("MONITORING-REQ-004: 업데이트 결과가 없는 차량은 결과 필드를 null 로 응답한다")
    void respondsNullResultFieldsWithoutUpdateResult() throws Exception {
        VehicleStatus vehicle =
                new VehicleStatus("vehicle-001", "model-a", "1.0.0", null, Instant.parse(UPDATED_AT), null, null, null);
        given(vehicleStatusQueryService.getSnapshot())
                .willReturn(new VehicleStatusSnapshot(Instant.parse(AS_OF), List.of(vehicle)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicles[0].lastSeenAt").value(nullValue()))
                .andExpect(jsonPath("$.vehicles[0].campaignId").value(nullValue()))
                .andExpect(jsonPath("$.vehicles[0].lastResult").value(nullValue()))
                .andExpect(jsonPath("$.vehicles[0].failureReason").value(nullValue()));
    }

    @Test
    @DisplayName("MONITORING-REQ-004: 차량이 없으면 빈 목록을 200 으로 응답한다")
    void respondsEmptyVehicles() throws Exception {
        given(vehicleStatusQueryService.getSnapshot())
                .willReturn(new VehicleStatusSnapshot(Instant.parse(AS_OF), List.of()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value(AS_OF))
                .andExpect(jsonPath("$.vehicles").isEmpty());
    }
}
