package com.orbit.server.domain.monitoring.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.domain.monitoring.service.VehicleDetail;
import com.orbit.server.domain.monitoring.service.VehicleLastLocation;
import com.orbit.server.domain.monitoring.service.VehicleLastUpdate;
import com.orbit.server.domain.monitoring.service.VehicleStatus;
import com.orbit.server.domain.monitoring.service.VehicleStatusQueryService;
import com.orbit.server.domain.monitoring.service.VehicleStatusSnapshot;
import com.orbit.server.domain.monitoring.service.VehicleStatusStreamService;
import com.orbit.server.domain.monitoring.service.VehicleUpdateAttempt;
import com.orbit.server.domain.monitoring.service.VehicleUpdateHistoryService;
import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.common.SuccessCode;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.CommonErrorCode;
import com.orbit.server.global.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class DashboardVehicleControllerTest {

    private static final String URL = "/api/v1/dashboard/vehicles";
    private static final String STREAM_URL = "/api/v1/dashboard/vehicles/stream";
    private static final String DETAIL_URL = "/api/v1/dashboard/vehicles/{vehicleId}";
    private static final String EVENTS_URL = "/api/v1/dashboard/vehicles/{vehicleId}/events";
    private static final String LAST_EVENT_ID = "Last-Event-ID";
    private static final String AS_OF = "2026-10-11T07:00:00.123Z";
    private static final String LAST_SEEN_AT = "2026-10-11T06:59:00.456Z";
    private static final String UPDATED_AT = "2026-10-11T06:59:00.789Z";

    private VehicleStatusQueryService vehicleStatusQueryService;
    private VehicleStatusStreamService vehicleStatusStreamService;
    private VehicleUpdateHistoryService vehicleUpdateHistoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        vehicleStatusQueryService = mock(VehicleStatusQueryService.class);
        vehicleStatusStreamService = mock(VehicleStatusStreamService.class);
        vehicleUpdateHistoryService = mock(VehicleUpdateHistoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardVehicleController(
                        vehicleStatusQueryService, vehicleStatusStreamService, vehicleUpdateHistoryService))
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
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(SuccessCode.OK.getCode()))
                .andExpect(jsonPath("$.data.asOf").value(AS_OF))
                .andExpect(jsonPath("$.data.vehicles.length()").value(1))
                .andExpect(jsonPath("$.data.vehicles[0].vehicleId").value("vehicle-001"))
                .andExpect(jsonPath("$.data.vehicles[0].model").value("model-a"))
                .andExpect(jsonPath("$.data.vehicles[0].currentVersion").value("1.0.0"))
                .andExpect(jsonPath("$.data.vehicles[0].lastSeenAt").value(LAST_SEEN_AT))
                .andExpect(jsonPath("$.data.vehicles[0].updatedAt").value(UPDATED_AT))
                .andExpect(jsonPath("$.data.vehicles[0].campaignId").value(1))
                .andExpect(jsonPath("$.data.vehicles[0].lastResult").value("FAILED"))
                .andExpect(jsonPath("$.data.vehicles[0].failureReason").value("HASH_MISMATCH"));
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
                .andExpect(jsonPath("$.data.vehicles[0].lastSeenAt").value(nullValue()))
                .andExpect(jsonPath("$.data.vehicles[0].campaignId").value(nullValue()))
                .andExpect(jsonPath("$.data.vehicles[0].lastResult").value(nullValue()))
                .andExpect(jsonPath("$.data.vehicles[0].failureReason").value(nullValue()));
    }

    @Test
    @DisplayName("MONITORING-REQ-004: 차량이 없으면 빈 목록을 200 으로 응답한다")
    void respondsEmptyVehicles() throws Exception {
        given(vehicleStatusQueryService.getSnapshot())
                .willReturn(new VehicleStatusSnapshot(Instant.parse(AS_OF), List.of()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.asOf").value(AS_OF))
                .andExpect(jsonPath("$.data.vehicles").isEmpty());
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 처음 연결하면 이벤트 스트림을 연다")
    void opensStream() throws Exception {
        given(vehicleStatusStreamService.subscribe(Optional.empty())).willReturn(new SseEmitter());

        mockMvc.perform(get(STREAM_URL).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 재연결하면 Last-Event-ID 를 기준 시각으로 넘긴다")
    void passesLastEventId() throws Exception {
        given(vehicleStatusStreamService.subscribe(Optional.of(Instant.parse(AS_OF))))
                .willReturn(new SseEmitter());

        mockMvc.perform(get(STREAM_URL).accept(MediaType.TEXT_EVENT_STREAM).header(LAST_EVENT_ID, AS_OF))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(vehicleStatusStreamService).subscribe(Optional.of(Instant.parse(AS_OF)));
    }

    @Test
    @DisplayName("MONITORING-REQ-001: Last-Event-ID 가 시각 형식이 아니면 400 INVALID_INPUT 으로 응답한다")
    void rejectsMalformedLastEventId() throws Exception {
        mockMvc.perform(get(STREAM_URL).accept(MediaType.TEXT_EVENT_STREAM).header(LAST_EVENT_ID, "not-a-time"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));

        verifyNoInteractions(vehicleStatusStreamService);
    }

    // 차량 상세는 보조 API(제안)라 요구사항 ID 가 없다
    @Test
    @DisplayName("차량 상세: 차량 정보·위치·배정 Edge·최근 업데이트 결과를 200 으로 응답한다")
    void respondsDetail() throws Exception {
        VehicleDetail detail = new VehicleDetail(
                "vehicle-001",
                "model-a",
                "hw-1",
                "KR",
                "1.0.0",
                Instant.parse(LAST_SEEN_AT),
                Instant.parse(UPDATED_AT),
                new VehicleLastLocation(37.5665, 126.978, Instant.parse(LAST_SEEN_AT)),
                "edge-seoul",
                new VehicleLastUpdate(
                        7L, "FAILED", "SIGNATURE_INVALID", "unknown keyId: v2", "edge-seoul", Instant.parse(AS_OF)));
        given(vehicleStatusQueryService.getDetail("vehicle-001")).willReturn(detail);

        mockMvc.perform(get(DETAIL_URL, "vehicle-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SuccessCode.OK.getCode()))
                .andExpect(jsonPath("$.data.vehicleId").value("vehicle-001"))
                .andExpect(jsonPath("$.data.model").value("model-a"))
                .andExpect(jsonPath("$.data.hwVersion").value("hw-1"))
                .andExpect(jsonPath("$.data.region").value("KR"))
                .andExpect(jsonPath("$.data.currentVersion").value("1.0.0"))
                .andExpect(jsonPath("$.data.lastSeenAt").value(LAST_SEEN_AT))
                .andExpect(jsonPath("$.data.updatedAt").value(UPDATED_AT))
                .andExpect(jsonPath("$.data.location.latitude").value(37.5665))
                .andExpect(jsonPath("$.data.location.longitude").value(126.978))
                .andExpect(jsonPath("$.data.location.updatedAt").value(LAST_SEEN_AT))
                .andExpect(jsonPath("$.data.assignedEdgeId").value("edge-seoul"))
                .andExpect(jsonPath("$.data.lastUpdate.campaignId").value(7))
                .andExpect(jsonPath("$.data.lastUpdate.result").value("FAILED"))
                .andExpect(jsonPath("$.data.lastUpdate.failureReason").value("SIGNATURE_INVALID"))
                .andExpect(jsonPath("$.data.lastUpdate.failureDetail").value("unknown keyId: v2"))
                .andExpect(jsonPath("$.data.lastUpdate.edgeId").value("edge-seoul"))
                .andExpect(jsonPath("$.data.lastUpdate.finishedAt").value(AS_OF));
    }

    @Test
    @DisplayName("차량 상세: 위치·업데이트 결과가 없으면 location 과 lastUpdate 를 null 로 응답한다")
    void respondsDetailWithoutLocationAndLastUpdate() throws Exception {
        VehicleDetail detail = new VehicleDetail(
                "vehicle-001", "model-a", "hw-1", "KR", "1.0.0", null, Instant.parse(UPDATED_AT), null, null, null);
        given(vehicleStatusQueryService.getDetail("vehicle-001")).willReturn(detail);

        mockMvc.perform(get(DETAIL_URL, "vehicle-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleId").value("vehicle-001"))
                .andExpect(jsonPath("$.data.lastSeenAt").value(nullValue()))
                .andExpect(jsonPath("$.data.location").value(nullValue()))
                .andExpect(jsonPath("$.data.assignedEdgeId").value(nullValue()))
                .andExpect(jsonPath("$.data.lastUpdate").value(nullValue()));
    }

    @Test
    @DisplayName("차량 상세: 차량이 없으면 404 VEHICLE_NOT_FOUND")
    void respondsDetailNotFound() throws Exception {
        given(vehicleStatusQueryService.getDetail("vehicle-999"))
                .willThrow(new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));

        mockMvc.perform(get(DETAIL_URL, "vehicle-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(VehicleErrorCode.VEHICLE_NOT_FOUND.getCode()));
    }

    // 차량 업데이트 이력은 보조 API(제안)라 요구사항 ID 가 없다
    @Test
    @DisplayName("차량 업데이트 이력: 버전별 업데이트 시도를 조회한 순서 그대로 200 으로 응답한다")
    void respondsUpdateHistory() throws Exception {
        given(vehicleUpdateHistoryService.getRecentAttempts("vehicle-001"))
                .willReturn(List.of(
                        new VehicleUpdateAttempt(
                                7L, "1.0.2", "SUCCEEDED", null, null, "edge-seoul", Instant.parse(UPDATED_AT)),
                        new VehicleUpdateAttempt(
                                7L,
                                "1.0.2",
                                "FAILED",
                                "HASH_MISMATCH",
                                "sha256 mismatch",
                                "edge-daejeon",
                                Instant.parse(LAST_SEEN_AT))));

        mockMvc.perform(get(EVENTS_URL, "vehicle-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SuccessCode.OK.getCode()))
                .andExpect(jsonPath("$.data.events.length()").value(2))
                .andExpect(jsonPath("$.data.events[0].campaignId").value(7))
                .andExpect(jsonPath("$.data.events[0].version").value("1.0.2"))
                .andExpect(jsonPath("$.data.events[0].result").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.events[0].failureReason").value(nullValue()))
                .andExpect(jsonPath("$.data.events[0].edgeId").value("edge-seoul"))
                .andExpect(jsonPath("$.data.events[0].finishedAt").value(UPDATED_AT))
                .andExpect(jsonPath("$.data.events[1].result").value("FAILED"))
                .andExpect(jsonPath("$.data.events[1].failureReason").value("HASH_MISMATCH"))
                .andExpect(jsonPath("$.data.events[1].failureDetail").value("sha256 mismatch"))
                .andExpect(jsonPath("$.data.events[1].finishedAt").value(LAST_SEEN_AT));
    }

    @Test
    @DisplayName("차량 업데이트 이력: 시도한 적이 없으면 빈 목록을 200 으로 응답한다")
    void respondsEmptyUpdateHistory() throws Exception {
        given(vehicleUpdateHistoryService.getRecentAttempts("vehicle-001")).willReturn(List.of());

        mockMvc.perform(get(EVENTS_URL, "vehicle-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events").isEmpty());
    }

    @Test
    @DisplayName("차량 업데이트 이력: 차량이 없으면 404 VEHICLE_NOT_FOUND")
    void respondsUpdateHistoryNotFound() throws Exception {
        given(vehicleUpdateHistoryService.getRecentAttempts("vehicle-999"))
                .willThrow(new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));

        mockMvc.perform(get(EVENTS_URL, "vehicle-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(VehicleErrorCode.VEHICLE_NOT_FOUND.getCode()));
    }
}
