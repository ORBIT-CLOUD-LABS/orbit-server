package com.orbit.server.domain.campaign.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.domain.campaign.entity.CampaignName;
import com.orbit.server.domain.campaign.entity.CampaignPeriod;
import com.orbit.server.domain.campaign.entity.CampaignStatus;
import com.orbit.server.domain.campaign.entity.TargetRelease;
import com.orbit.server.domain.campaign.entity.VersionRange;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.campaign.service.CampaignDeactivationService;
import com.orbit.server.domain.campaign.service.CampaignDetail;
import com.orbit.server.domain.campaign.service.CampaignDetailService;
import com.orbit.server.domain.campaign.service.CampaignFilter;
import com.orbit.server.domain.campaign.service.CampaignListItem;
import com.orbit.server.domain.campaign.service.CampaignListService;
import com.orbit.server.domain.campaign.service.CampaignRegistrationService;
import com.orbit.server.domain.campaign.service.CampaignSummary;
import com.orbit.server.domain.campaign.service.CampaignTargetDetail;
import com.orbit.server.domain.campaign.service.RegisterCampaignCommand;
import com.orbit.server.global.common.SuccessCode;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.CommonErrorCode;
import com.orbit.server.global.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CampaignAdminControllerTest {

    private static final String URL = "/api/v1/admin/campaigns";
    private static final String START_AT = "2026-10-01T00:00:00Z";
    private static final String END_AT = "2026-10-02T00:00:00Z";
    private static final String CREATED_AT = "2026-09-30T07:00:00.123Z";
    private static final String TARGET = """
            {"model": "model-a", "hwVersions": ["hw-1"], "regions": ["KR"],
             "currentVersionMin": "1.0.0", "currentVersionMax": "1.0.9"}""";

    private CampaignRegistrationService campaignRegistrationService;
    private CampaignListService campaignListService;
    private CampaignDetailService campaignDetailService;
    private CampaignDeactivationService campaignDeactivationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        campaignRegistrationService = mock(CampaignRegistrationService.class);
        campaignListService = mock(CampaignListService.class);
        campaignDetailService = mock(CampaignDetailService.class);
        campaignDeactivationService = mock(CampaignDeactivationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CampaignAdminController(
                        campaignRegistrationService,
                        campaignListService,
                        campaignDetailService,
                        campaignDeactivationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 보조 API(제안)라 요구사항 ID 가 없다
    @Test
    @DisplayName("캠페인 목록: 캠페인과 목표 버전을 200 으로 응답한다")
    void respondsCampaigns() throws Exception {
        CampaignSummary campaign = new CampaignSummary(
                1L,
                10L,
                "campaign",
                "model-a",
                CampaignStatus.ACTIVE,
                Instant.parse(START_AT),
                Instant.parse(END_AT),
                Instant.parse(CREATED_AT));
        given(campaignListService.findAll(any())).willReturn(List.of(new CampaignListItem(campaign, "1.0.1")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(SuccessCode.OK.getCode()))
                .andExpect(jsonPath("$.data.campaigns.length()").value(1))
                .andExpect(jsonPath("$.data.campaigns[0].campaignId").value(1))
                .andExpect(jsonPath("$.data.campaigns[0].name").value("campaign"))
                .andExpect(jsonPath("$.data.campaigns[0].model").value("model-a"))
                .andExpect(jsonPath("$.data.campaigns[0].targetVersion").value("1.0.1"))
                .andExpect(jsonPath("$.data.campaigns[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.campaigns[0].startAt").value(START_AT))
                .andExpect(jsonPath("$.data.campaigns[0].endAt").value(END_AT))
                .andExpect(jsonPath("$.data.campaigns[0].createdAt").value(CREATED_AT));
    }

    @ParameterizedTest
    @CsvSource(
            nullValues = "NULL",
            value = {"NULL, NULL", "INACTIVE, NULL", "NULL, model-a", "ACTIVE, model-a"})
    @DisplayName("캠페인 목록: 상태·차종 필터 조합을 그대로 넘긴다")
    void passesFilter(CampaignStatus status, String model) throws Exception {
        given(campaignListService.findAll(any())).willReturn(List.of());
        MockHttpServletRequestBuilder request = get(URL);
        if (status != null) {
            request.param("status", status.name());
        }
        if (model != null) {
            request.param("model", model);
        }

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.campaigns").isEmpty());

        verify(campaignListService).findAll(new CampaignFilter(status, model));
    }

    @Test
    @DisplayName("캠페인 목록: 정의되지 않은 상태로 필터링하면 400 INVALID_INPUT 이고 조회하지 않는다")
    void rejectsUnknownStatus() throws Exception {
        mockMvc.perform(get(URL).param("status", "PAUSED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));

        verify(campaignListService, never()).findAll(any());
    }

    @Test
    @DisplayName("캠페인 상세: 대상 조건·기간·상태·목표 버전을 200 으로 응답한다")
    void respondsCampaignDetail() throws Exception {
        CampaignTargetDetail target =
                new CampaignTargetDetail("model-a", List.of("hw-1", "hw-2"), List.of("KR"), "1.0.0", null);
        given(campaignDetailService.getById(1L))
                .willReturn(new CampaignDetail(
                        1L,
                        "campaign",
                        10L,
                        "1.1.0",
                        target,
                        CampaignStatus.ACTIVE,
                        Instant.parse(START_AT),
                        Instant.parse(END_AT),
                        Instant.parse(CREATED_AT)));

        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(SuccessCode.OK.getCode()))
                .andExpect(jsonPath("$.data.campaignId").value(1))
                .andExpect(jsonPath("$.data.name").value("campaign"))
                .andExpect(jsonPath("$.data.artifactId").value(10))
                .andExpect(jsonPath("$.data.targetVersion").value("1.1.0"))
                .andExpect(jsonPath("$.data.target.model").value("model-a"))
                .andExpect(jsonPath("$.data.target.hwVersions[0]").value("hw-1"))
                .andExpect(jsonPath("$.data.target.hwVersions[1]").value("hw-2"))
                .andExpect(jsonPath("$.data.target.regions[0]").value("KR"))
                .andExpect(jsonPath("$.data.target.currentVersionMin").value("1.0.0"))
                .andExpect(jsonPath("$.data.target.currentVersionMax").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.startAt").value(START_AT))
                .andExpect(jsonPath("$.data.endAt").value(END_AT))
                .andExpect(jsonPath("$.data.createdAt").value(CREATED_AT));
    }

    @Test
    @DisplayName("캠페인 상세: 캠페인이 없으면 404 CAMPAIGN_NOT_FOUND 로 응답한다")
    void respondsCampaignNotFound() throws Exception {
        given(campaignDetailService.getById(1L)).willThrow(new BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));

        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CampaignErrorCode.CAMPAIGN_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("캠페인 비활성화: 비활성화한 캠페인 상세를 200 으로 응답한다")
    void deactivatesCampaign() throws Exception {
        given(campaignDeactivationService.deactivate(1L))
                .willReturn(new CampaignDetail(
                        1L,
                        "campaign",
                        10L,
                        "1.1.0",
                        new CampaignTargetDetail("model-a", List.of(), List.of(), null, null),
                        CampaignStatus.INACTIVE,
                        Instant.parse(START_AT),
                        Instant.parse(END_AT),
                        Instant.parse(CREATED_AT)));

        performDeactivate("""
                        {"status": "INACTIVE"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(SuccessCode.OK.getCode()))
                .andExpect(jsonPath("$.data.campaignId").value(1))
                .andExpect(jsonPath("$.data.targetVersion").value("1.1.0"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("캠페인 비활성화: 캠페인이 없으면 404 CAMPAIGN_NOT_FOUND 로 응답한다")
    void respondsCampaignNotFoundOnDeactivate() throws Exception {
        given(campaignDeactivationService.deactivate(1L))
                .willThrow(new BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));

        performDeactivate("""
                        {"status": "INACTIVE"}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CampaignErrorCode.CAMPAIGN_NOT_FOUND.getCode()));
    }

    // 재활성화는 허용하지 않으므로 ACTIVE 도 정의되지 않은 값과 같이 요청 값 오류로 본다
    @ParameterizedTest
    @ValueSource(strings = {"{\"status\": \"ACTIVE\"}", "{\"status\": \"PAUSED\"}", "{\"status\": null}", "{}"})
    @DisplayName("캠페인 비활성화: 상태가 INACTIVE 가 아니면 400 INVALID_INPUT 이고 바꾸지 않는다")
    void rejectsStatusOtherThanInactive(String body) throws Exception {
        performDeactivate(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));

        verify(campaignDeactivationService, never()).deactivate(any());
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 캠페인을 등록하고 201 과 캠페인 ID 를 응답한다")
    void registersCampaign() throws Exception {
        given(campaignRegistrationService.register(any())).willReturn(1L);

        perform(body(START_AT, END_AT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(SuccessCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.campaignId").value(1));

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
    @DisplayName("CAMPAIGN-REQ-001: 현재 버전 범위가 점으로 나눈 숫자 형식이 아니면 400 INVALID_INPUT 이고 등록하지 않는다")
    void rejectsNonDottedVersionRange() throws Exception {
        String body = """
                {"name": "campaign", "artifactId": 1, "targetVersion": "1.0.1",
                 "target": {"model": "model-a", "hwVersions": ["hw-1"], "regions": ["KR"],
                            "currentVersionMin": "v1.0.0"},
                 "startAt": "%s", "endAt": "%s"}""".formatted(START_AT, END_AT);

        perform(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));

        verify(campaignRegistrationService, never()).register(any());
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 목표 버전이 점으로 나눈 숫자 형식이 아니면 400 INVALID_INPUT 이고 등록하지 않는다")
    void rejectsNonDottedTargetVersion() throws Exception {
        String body = """
                {"name": "campaign", "artifactId": 1, "targetVersion": "v1.0.1", "target": %s,
                 "startAt": "%s", "endAt": "%s"}""".formatted(TARGET, START_AT, END_AT);

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

    private ResultActions performDeactivate(String body) throws Exception {
        return mockMvc.perform(
                patch(URL + "/1").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String body(String startAt, String endAt) {
        return """
                {"name": "campaign", "artifactId": 1, "targetVersion": "1.0.1", "target": %s,
                 "startAt": "%s", "endAt": "%s"}""".formatted(TARGET, startAt, endAt);
    }
}
