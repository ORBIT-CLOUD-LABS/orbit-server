package com.orbit.server.domain.update.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactFile;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.entity.CampaignCondition;
import com.orbit.server.domain.campaign.entity.CampaignName;
import com.orbit.server.domain.campaign.entity.CampaignPeriod;
import com.orbit.server.domain.campaign.entity.CampaignTarget;
import com.orbit.server.domain.campaign.entity.VersionRange;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.service.RegisterVehicleCommand;
import com.orbit.server.domain.vehicle.service.VehicleRegistrationService;
import com.orbit.server.global.error.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, ManifestServiceTest.FixedClockConfig.class})
@Transactional
class ManifestServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final String VEHICLE_ID = "vehicle-001";
    private static final String SHA256 = "a".repeat(64);
    private static final long FILE_SIZE = 1024;

    @Autowired
    private ManifestService service;

    @Autowired
    private VehicleRegistrationService registrationService;

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        registrationService.register(new RegisterVehicleCommand(
                new VehicleExternalId(VEHICLE_ID), new VehicleSpec("model-a", "hw-1", "KR"), "1.0.0"));
    }

    @Test
    @DisplayName("MANIFEST-REQ-001: 대상 차량에 파일 정보와 CDN 다운로드 URL 을 발급한다")
    void issuesManifest() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW));

        ManifestResult result = service.issue(VEHICLE_ID, campaignId);

        assertThat(result.campaignId()).isEqualTo(campaignId);
        assertThat(result.targetVersion()).isEqualTo("1.0.1");
        assertThat(result.fileSize()).isEqualTo(FILE_SIZE);
        assertThat(result.sha256()).isEqualTo(SHA256);
        assertThat(result.downloadUrl()).isEqualTo("http://cdn.test/firmware/model-a/1.0.1/firmware.bin");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 마지막 체크인 때 기록한 현재 버전으로 대상을 다시 판정한다")
    void judgesWithVersionRecordedAtLastCheckIn() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW));
        checkInService.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.1"));

        assertThatThrownBy(() -> service.issue(VEHICLE_ID, campaignId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CampaignErrorCode.NOT_UPDATE_TARGET);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 주입한 시계의 시각으로 적용 기간을 판정한다")
    void judgesPeriodWithInjectedClock() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW.plus(Duration.ofDays(7))));

        assertThatThrownBy(() -> service.issue(VEHICLE_ID, campaignId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CampaignErrorCode.CAMPAIGN_INACTIVE);
    }

    private Long registerCampaign(String targetVersion, CampaignPeriod period) {
        ArtifactRelease release = new ArtifactRelease("model-a", targetVersion);
        Artifact artifact = new Artifact(release, new ArtifactFile(SHA256, FILE_SIZE, release.originPath()));
        em.persist(artifact);
        CampaignTarget target = new CampaignTarget("model-a", Set.of("hw-1"), Set.of("KR"));
        CampaignCondition condition = new CampaignCondition(target, VersionRange.unbounded(), period);
        Long campaignId = campaignRepository
                .save(new Campaign(new CampaignName("campaign"), artifact.id(), condition))
                .id();
        em.flush();
        em.clear();
        return campaignId;
    }

    private static CampaignPeriod periodAround(Instant time) {
        return new CampaignPeriod(time.minus(Duration.ofHours(1)), time.plus(Duration.ofHours(1)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
