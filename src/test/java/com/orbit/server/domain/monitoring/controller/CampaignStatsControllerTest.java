package com.orbit.server.domain.monitoring.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.monitoring.service.CampaignResultCount;
import com.orbit.server.domain.monitoring.service.CampaignResultCounts;
import com.orbit.server.domain.monitoring.service.CampaignStats;
import com.orbit.server.domain.monitoring.service.CampaignStatsService;
import com.orbit.server.global.common.SuccessCode;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CampaignStatsControllerTest {

    private static final String URL = "/api/v1/admin/campaigns/{campaignId}/stats";

    private CampaignStatsService campaignStatsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        campaignStatsService = mock(CampaignStatsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CampaignStatsController(campaignStatsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 보조 API(제안)라 요구사항 ID 가 없다
    @Test
    @DisplayName("캠페인 진행 현황: 대상·성공·실패(사유별)·진행 중 차량 수를 200 으로 응답한다")
    void respondsStats() throws Exception {
        CampaignResultCounts results = new CampaignResultCounts(List.of(
                new CampaignResultCount("SUCCEEDED", null, 80),
                new CampaignResultCount("FAILED", "DOWNLOAD_FAILED", 2),
                new CampaignResultCount("FAILED", "SIGNATURE_INVALID", 1),
                new CampaignResultCount("FAILED", "INSTALL_FAILED", 2)));
        given(campaignStatsService.getStats(7L)).willReturn(CampaignStats.of(7L, results, 15));

        mockMvc.perform(get(URL, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(SuccessCode.OK.getCode()))
                .andExpect(jsonPath("$.data.campaignId").value(7))
                .andExpect(jsonPath("$.data.targetVehicles").value(100))
                .andExpect(jsonPath("$.data.succeeded").value(80))
                .andExpect(jsonPath("$.data.failed").value(5))
                .andExpect(jsonPath("$.data.failureReasons.DOWNLOAD_FAILED").value(2))
                .andExpect(jsonPath("$.data.failureReasons.HASH_MISMATCH").value(0))
                .andExpect(jsonPath("$.data.failureReasons.SIGNATURE_INVALID").value(1))
                .andExpect(jsonPath("$.data.failureReasons.DOWNGRADE_REJECTED").value(0))
                .andExpect(jsonPath("$.data.failureReasons.INSTALL_FAILED").value(2))
                .andExpect(jsonPath("$.data.inProgress").value(15));
    }

    @Test
    @DisplayName("캠페인 진행 현황: 캠페인이 없으면 404 CAMPAIGN_NOT_FOUND")
    void respondsNotFound() throws Exception {
        given(campaignStatsService.getStats(7L)).willThrow(new BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));

        mockMvc.perform(get(URL, 7L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CampaignErrorCode.CAMPAIGN_NOT_FOUND.getCode()));
    }
}
