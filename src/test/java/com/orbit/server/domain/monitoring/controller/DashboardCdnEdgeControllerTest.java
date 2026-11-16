package com.orbit.server.domain.monitoring.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.domain.monitoring.service.CdnEdgeStatus;
import com.orbit.server.domain.monitoring.service.CdnEdgeStatusService;
import com.orbit.server.domain.monitoring.service.EdgeDownloadCount;
import com.orbit.server.global.common.SuccessCode;
import com.orbit.server.global.error.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardCdnEdgeControllerTest {

    private static final String URL = "/api/v1/dashboard/cdn-edges";

    private CdnEdgeStatusService cdnEdgeStatusService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cdnEdgeStatusService = mock(CdnEdgeStatusService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardCdnEdgeController(cdnEdgeStatusService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("CDN Edge 상태: Edge 별 위치·정상 여부·배정 차량 수·캠페인별 다운로드 수를 설정 순서대로 200 으로 응답한다")
    void respondsEdges() throws Exception {
        given(cdnEdgeStatusService.getStatuses())
                .willReturn(List.of(
                        new CdnEdgeStatus(
                                "edge-seoul",
                                "서울",
                                37.5665,
                                126.9780,
                                true,
                                42,
                                List.of(
                                        new EdgeDownloadCount("edge-seoul", 7L, 40, 2),
                                        new EdgeDownloadCount("edge-seoul", 9L, 3, 0))),
                        new CdnEdgeStatus("edge-busan", "부산", 35.1796, 129.0756, false, 0, List.of())));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(SuccessCode.OK.getCode()))
                .andExpect(jsonPath("$.data.edges.length()").value(2))
                .andExpect(jsonPath("$.data.edges[0].edgeId").value("edge-seoul"))
                .andExpect(jsonPath("$.data.edges[0].name").value("서울"))
                .andExpect(jsonPath("$.data.edges[0].latitude").value(37.5665))
                .andExpect(jsonPath("$.data.edges[0].longitude").value(126.9780))
                .andExpect(jsonPath("$.data.edges[0].healthy").value(true))
                .andExpect(jsonPath("$.data.edges[0].assignedVehicles").value(42))
                .andExpect(jsonPath("$.data.edges[0].downloads.length()").value(2))
                .andExpect(jsonPath("$.data.edges[0].downloads[0].campaignId").value(7))
                .andExpect(jsonPath("$.data.edges[0].downloads[0].succeeded").value(40))
                .andExpect(jsonPath("$.data.edges[0].downloads[0].failed").value(2))
                .andExpect(jsonPath("$.data.edges[0].downloads[1].campaignId").value(9))
                .andExpect(jsonPath("$.data.edges[1].edgeId").value("edge-busan"))
                .andExpect(jsonPath("$.data.edges[1].healthy").value(false))
                .andExpect(jsonPath("$.data.edges[1].assignedVehicles").value(0))
                .andExpect(jsonPath("$.data.edges[1].downloads").isEmpty());
    }

    @Test
    @DisplayName("CDN Edge 상태: 내부 주소와 다운로드 항목의 Edge id 는 응답에 넣지 않는다")
    void hidesInternalFields() throws Exception {
        given(cdnEdgeStatusService.getStatuses())
                .willReturn(List.of(new CdnEdgeStatus(
                        "edge-seoul",
                        "서울",
                        37.5665,
                        126.9780,
                        true,
                        1,
                        List.of(new EdgeDownloadCount("edge-seoul", 7L, 1, 0)))));

        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$.data.edges[0].baseUrl").doesNotExist())
                .andExpect(jsonPath("$.data.edges[0].downloads[0].edgeId").doesNotExist());
    }
}
