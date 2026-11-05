package com.orbit.server.domain.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.domain.artifact.service.ArtifactQueryService;
import com.orbit.server.domain.artifact.service.ArtifactSummary;
import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.entity.CampaignName;
import com.orbit.server.domain.campaign.entity.CampaignPeriod;
import com.orbit.server.domain.campaign.entity.TargetRelease;
import com.orbit.server.domain.campaign.entity.VersionRange;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.InvalidValueException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CampaignRegistrationServiceTest {

    private static final Long ARTIFACT_ID = 1L;
    private static final Long MISSING_ARTIFACT_ID = 2L;
    private static final ArtifactSummary ARTIFACT = new ArtifactSummary(
            ARTIFACT_ID, "model-a", "1.0.1", 1, "a".repeat(64), "/firmware/model-a/1.0.1/firmware.bin", Instant.EPOCH);
    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final CampaignPeriod PERIOD = new CampaignPeriod(START, START.plus(Duration.ofDays(1)));

    private CampaignRepository campaignRepository;
    private CampaignRegistrationService service;

    @BeforeEach
    void setUp() {
        ArtifactQueryService artifactQueryService = mock(ArtifactQueryService.class);
        given(artifactQueryService.lockAndGetById(ARTIFACT_ID)).willReturn(ARTIFACT);
        given(artifactQueryService.lockAndGetById(MISSING_ARTIFACT_ID))
                .willThrow(new BusinessException(ArtifactErrorCode.ARTIFACT_NOT_FOUND));
        campaignRepository = mock(CampaignRepository.class);
        // ID 는 DB 가 채우므로 저장할 때 흉내 낸다
        given(campaignRepository.save(any())).willAnswer(invocation -> {
            Campaign campaign = invocation.getArgument(0);
            ReflectionTestUtils.setField(campaign, "id", 1L);
            return campaign;
        });
        service = new CampaignRegistrationService(artifactQueryService, campaignRepository);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 파일과 차종·목표 버전이 맞으면 활성 캠페인으로 등록한다")
    void registersCampaign() {
        Long campaignId = service.register(commandOf(ARTIFACT_ID, new TargetRelease("model-a", "1.0.1")));

        assertThat(campaignId).isEqualTo(1L);
        verify(campaignRepository).save(any());
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 파일이 없으면 404 ARTIFACT_NOT_FOUND 이고 저장하지 않는다")
    void rejectsMissingArtifact() {
        RegisterCampaignCommand command = commandOf(MISSING_ARTIFACT_ID, new TargetRelease("model-a", "1.0.1"));

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.ARTIFACT_NOT_FOUND);
        verify(campaignRepository, never()).save(any());
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 목표 버전이 파일 버전과 다르면 400 이고 저장하지 않는다")
    void rejectsDifferentTargetVersion() {
        RegisterCampaignCommand command = commandOf(ARTIFACT_ID, new TargetRelease("model-a", "1.0.0"));

        assertThatThrownBy(() -> service.register(command)).isInstanceOf(InvalidValueException.class);
        verify(campaignRepository, never()).save(any());
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 대상 차종이 파일 차종과 다르면 400 이고 저장하지 않는다")
    void rejectsDifferentModel() {
        RegisterCampaignCommand command = commandOf(ARTIFACT_ID, new TargetRelease("model-b", "1.0.1"));

        assertThatThrownBy(() -> service.register(command)).isInstanceOf(InvalidValueException.class);
        verify(campaignRepository, never()).save(any());
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 같은 대상 조건과 목표 버전의 활성 캠페인이 있으면 409 ACTIVE_CAMPAIGN_EXISTS 이고 저장하지 않는다")
    void rejectsDuplicateActiveCampaign() {
        RegisterCampaignCommand command = commandOf(ARTIFACT_ID, new TargetRelease("model-a", "1.0.1"));
        // 대상 차종 필터는 CampaignRepositoryTest 에서 검증하므로 같은 조건의 활성 캠페인이 있다고 둔다
        given(campaignRepository.findAllActiveByModel("model-a")).willReturn(List.of(command.toCampaign()));

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CampaignErrorCode.ACTIVE_CAMPAIGN_EXISTS);
        verify(campaignRepository, never()).save(any());
    }

    private static RegisterCampaignCommand commandOf(Long artifactId, TargetRelease release) {
        return new RegisterCampaignCommand(
                new CampaignName("campaign"),
                artifactId,
                release,
                Set.of("hw-1"),
                Set.of("KR"),
                VersionRange.unbounded(),
                PERIOD);
    }
}
