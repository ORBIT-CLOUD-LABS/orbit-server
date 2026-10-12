package com.orbit.server.domain.campaign.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.domain.campaign.entity.CampaignName;
import com.orbit.server.domain.campaign.entity.CampaignPeriod;
import com.orbit.server.domain.campaign.entity.TargetRelease;
import com.orbit.server.domain.campaign.entity.VersionRange;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.campaign.service.CampaignRegistrationService;
import com.orbit.server.domain.campaign.service.RegisterCampaignCommand;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.CommonErrorCode;
import com.orbit.server.global.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CampaignAdminControllerTest {

    private static final String URL = "/api/v1/admin/campaigns";
    private static final String START_AT = "2026-10-01T00:00:00Z";
    private static final String END_AT = "2026-10-02T00:00:00Z";
    private static final String TARGET = """
            {"model": "model-a", "hwVersions": ["hw-1"], "regions": ["KR"],
             "currentVersionMin": "1.0.0", "currentVersionMax": "1.0.9"}""";

    private CampaignRegistrationService campaignRegistrationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        campaignRegistrationService = mock(CampaignRegistrationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CampaignAdminController(campaignRegistrationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 캠페인을 등록하고 201 과 캠페인 ID 를 응답한다")
    void registersCampaign() throws Exception {
        given(campaignRegistrationService.register(any())).willReturn(1L);

        perform(body(START_AT, END_AT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId").value(1));

        ArgumentCaptor<RegisterCampaignCommand> captor = ArgumentCaptor.forClass(RegisterCampaignCommand.class);
        verify(campaignRegistrationService).register(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(new RegisterCampaignCommand(
                        new CampaignName("campaign"),
                        1L,
                        new TargetRelease("model-a", "1.0.1"),
                        Set.of("hw-1"),
                        Set.of("KR"),
                        new VersionRange("1.0.0", "1.0.9"),
                        new CampaignPeriod(Instant.parse(START_AT), Instant.parse(END_AT))));
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 시작 시각이 종료 시각보다 늦거나 같으면 400 INVALID_INPUT 이고 등록하지 않는다")
    void rejectsInvalidPeriod() throws Exception {
        perform(body(END_AT, START_AT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));

        verify(campaignRegistrationService, never()).register(any());
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 대상 조건이 없으면 400 INVALID_INPUT 이고 등록하지 않는다")
    void rejectsMissingTarget() throws Exception {
        String body = """
                {"name": "campaign", "artifactId": 1, "targetVersion": "1.0.1",
                 "startAt": "%s", "endAt": "%s"}""".formatted(START_AT, END_AT);

        perform(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));

        verify(campaignRegistrationService, never()).register(any());
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 파일이 없으면 404 ARTIFACT_NOT_FOUND 로 응답한다")
    void respondsArtifactNotFound() throws Exception {
        given(campaignRegistrationService.register(any()))
                .willThrow(new BusinessException(ArtifactErrorCode.ARTIFACT_NOT_FOUND));

        perform(body(START_AT, END_AT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ArtifactErrorCode.ARTIFACT_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 중복 활성 캠페인이 있으면 409 ACTIVE_CAMPAIGN_EXISTS 로 응답한다")
    void respondsActiveCampaignExists() throws Exception {
        given(campaignRegistrationService.register(any()))
                .willThrow(new BusinessException(CampaignErrorCode.ACTIVE_CAMPAIGN_EXISTS));

        perform(body(START_AT, END_AT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(CampaignErrorCode.ACTIVE_CAMPAIGN_EXISTS.getCode()));
    }

    private ResultActions perform(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String body(String startAt, String endAt) {
        return """
                {"name": "campaign", "artifactId": 1, "targetVersion": "1.0.1", "target": %s,
                 "startAt": "%s", "endAt": "%s"}""".formatted(TARGET, startAt, endAt);
    }
}
