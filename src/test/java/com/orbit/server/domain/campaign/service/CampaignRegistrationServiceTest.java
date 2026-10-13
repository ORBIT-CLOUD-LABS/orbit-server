package com.orbit.server.domain.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.domain.artifact.repository.ArtifactRepository;
import com.orbit.server.domain.artifact.service.ArtifactQueryService;
import com.orbit.server.domain.artifact.service.ArtifactSummary;
import com.orbit.server.domain.artifact.service.ArtifactSummaryQuery;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CampaignRegistrationServiceTest {

    private static final Long ARTIFACT_ID = 1L;
    private static final ArtifactSummary ARTIFACT = new ArtifactSummary(
            ARTIFACT_ID, "model-a", "1.0.1", 1, "a".repeat(64), "/firmware/model-a/1.0.1/firmware.bin", Instant.EPOCH);
    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final CampaignPeriod PERIOD = new CampaignPeriod(START, START.plus(Duration.ofDays(1)));

    private FakeCampaignRepository campaignRepository;
    private CampaignRegistrationService service;

    @BeforeEach
    void setUp() {
        campaignRepository = new FakeCampaignRepository();
        ArtifactQueryService artifactQueryService = new ArtifactQueryService(
                new FakeArtifactSummaryQuery(ARTIFACT), new FakeArtifactRepository(ARTIFACT_ID));
        service = new CampaignRegistrationService(artifactQueryService, campaignRepository);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 파일과 차종·목표 버전이 맞으면 활성 캠페인으로 등록한다")
    void registersCampaign() {
        Long campaignId = service.register(commandOf(ARTIFACT_ID, new TargetRelease("model-a", "1.0.1")));

        assertThat(campaignId).isEqualTo(1L);
        assertThat(campaignRepository.saved).hasSize(1);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 파일이 없으면 404 ARTIFACT_NOT_FOUND 이고 저장하지 않는다")
    void rejectsMissingArtifact() {
        RegisterCampaignCommand command = commandOf(2L, new TargetRelease("model-a", "1.0.1"));

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.ARTIFACT_NOT_FOUND);
        assertThat(campaignRepository.saved).isEmpty();
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 목표 버전이 파일 버전과 다르면 400 이고 저장하지 않는다")
    void rejectsDifferentTargetVersion() {
        RegisterCampaignCommand command = commandOf(ARTIFACT_ID, new TargetRelease("model-a", "1.0.0"));

        assertThatThrownBy(() -> service.register(command)).isInstanceOf(InvalidValueException.class);
        assertThat(campaignRepository.saved).isEmpty();
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 대상 차종이 파일 차종과 다르면 400 이고 저장하지 않는다")
    void rejectsDifferentModel() {
        RegisterCampaignCommand command = commandOf(ARTIFACT_ID, new TargetRelease("model-b", "1.0.1"));

        assertThatThrownBy(() -> service.register(command)).isInstanceOf(InvalidValueException.class);
        assertThat(campaignRepository.saved).isEmpty();
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 같은 대상 조건과 목표 버전의 활성 캠페인이 있으면 409 ACTIVE_CAMPAIGN_EXISTS 이고 저장하지 않는다")
    void rejectsDuplicateActiveCampaign() {
        RegisterCampaignCommand command = commandOf(ARTIFACT_ID, new TargetRelease("model-a", "1.0.1"));
        service.register(command);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CampaignErrorCode.ACTIVE_CAMPAIGN_EXISTS);
        assertThat(campaignRepository.saved).hasSize(1);
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

    private record FakeArtifactSummaryQuery(ArtifactSummary artifact) implements ArtifactSummaryQuery {

        @Override
        public List<ArtifactSummary> findAll() {
            return List.of(artifact);
        }

        @Override
        public List<ArtifactSummary> findAllByModel(String model) {
            return List.of(artifact);
        }

        @Override
        public Optional<ArtifactSummary> findById(Long id) {
            return Optional.of(artifact).filter(summary -> summary.artifactId().equals(id));
        }

        // 캠페인 등록은 파일 한 건만 조회한다
        @Override
        public List<ArtifactSummary> findAllByIds(Collection<Long> ids) {
            throw new UnsupportedOperationException();
        }
    }

    private record FakeArtifactRepository(Long artifactId) implements ArtifactRepository {

        @Override
        public Optional<Long> lockById(Long id) {
            return Optional.of(artifactId).filter(id::equals);
        }

        // 캠페인 등록은 파일 등록 기능을 쓰지 않는다
        @Override
        public boolean existsByRelease(ArtifactRelease release) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Artifact save(Artifact artifact) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeCampaignRepository implements CampaignRepository {

        private final List<Campaign> saved = new ArrayList<>();

        // 대상 차종 필터는 CampaignJpaRepositoryTest 에서 검증하므로 저장한 캠페인을 모두 돌려준다
        @Override
        public List<Campaign> findAllActiveByModel(String model) {
            return List.copyOf(saved);
        }

        @Override
        public Campaign save(Campaign campaign) {
            saved.add(campaign);
            ReflectionTestUtils.setField(campaign, "id", (long) saved.size());
            return campaign;
        }

        // ID 는 저장 순번이다
        @Override
        public boolean existsById(Long campaignId) {
            return campaignId >= 1 && campaignId <= saved.size();
        }

        @Override
        public Optional<Campaign> findById(Long campaignId) {
            throw new UnsupportedOperationException();
        }
    }
}
