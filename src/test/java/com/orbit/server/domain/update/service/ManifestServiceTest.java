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
import com.orbit.server.domain.campaign.service.CampaignDeactivationService;
import com.orbit.server.domain.update.entity.VehicleLocation;
import com.orbit.server.domain.update.service.EdgeAssignment.FallbackUrl;
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
import java.util.Optional;
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
    private static final String PATH = "/firmware/model-a/1.0.1/firmware.bin";
    // 만료 시각은 NOW 의 1시간 뒤(Unix 초 1790859600)이고, 서명은 application-test.yml 의 비밀 값과 키 버전으로 만든 값이다
    private static final String SIGNED_PATH = PATH + "?md5=U3VCafgUdJHZwHsc5prjFw&expires=1790859600&kid=test";
    // 울산 근처: application-test.yml 의 Edge 중 부산이 가장 가깝다
    private static final VehicleLocation NEAR_BUSAN = new VehicleLocation(35.5384, 129.3114);

    @Autowired
    private ManifestService service;

    @Autowired
    private VehicleRegistrationService registrationService;

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignDeactivationService campaignDeactivationService;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        registrationService.register(new RegisterVehicleCommand(
                new VehicleExternalId(VEHICLE_ID), new VehicleSpec("model-a", "hw-1", "KR"), "1.0.0"));
    }

    @Test
    @DisplayName("MANIFEST-REQ-001: 대상 차량에 파일 정보와 Edge 다운로드 URL 을 발급한다")
    void issuesManifest() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW));

        ManifestResult result = service.issue(VEHICLE_ID, campaignId);

        assertThat(result.campaignId()).isEqualTo(campaignId);
        assertThat(result.targetVersion()).isEqualTo("1.0.1");
        assertThat(result.fileSize()).isEqualTo(FILE_SIZE);
        assertThat(result.sha256()).isEqualTo(SHA256);
        // 위치를 보낸 적이 없으므로 기본 Edge(서울)이고 나머지는 설정 순서다
        assertThat(result.edge().edgeId()).isEqualTo("edge-seoul");
        assertThat(result.edge().distanceKm()).isEmpty();
        assertThat(result.downloadUrl()).isEqualTo("http://cdn.test:30081" + SIGNED_PATH);
        assertThat(result.fallbackUrls())
                .containsExactly(
                        new FallbackUrl("edge-daejeon", "http://cdn.test:30082" + SIGNED_PATH),
                        new FallbackUrl("edge-busan", "http://cdn.test:30083" + SIGNED_PATH));
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 체크인으로 보낸 위치에서 가까운 Edge 를 고르고 배정 Edge 로 기록한다")
    void selectsNearestEdgeFromCheckInLocation() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW));
        checkInAt(NEAR_BUSAN);

        ManifestResult result = service.issue(VEHICLE_ID, campaignId);

        assertThat(result.edge().edgeId()).isEqualTo("edge-busan");
        assertThat(result.edge().distanceKm()).isPresent();
        assertThat(result.downloadUrl()).isEqualTo("http://cdn.test:30083" + SIGNED_PATH);
        assertThat(assignedEdgeId()).isEqualTo("edge-busan");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 대상이 아닌 차량에는 배정 Edge 를 기록하지 않는다")
    void doesNotAssignEdgeWhenNotTarget() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW));
        checkInService.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.1"));

        assertThatThrownBy(() -> service.issue(VEHICLE_ID, campaignId)).isInstanceOf(BusinessException.class);
        assertThat(assignedEdgeId()).isNull();
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
    @DisplayName("MANIFEST-POL-001: 비활성화한 캠페인이면 적용 기간 안이어도 CAMPAIGN_INACTIVE")
    void rejectsDeactivatedCampaign() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW));
        campaignDeactivationService.deactivate(campaignId);

        assertThatThrownBy(() -> service.issue(VEHICLE_ID, campaignId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CampaignErrorCode.CAMPAIGN_INACTIVE);
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 주입한 시계의 시각으로 적용 기간을 판정한다")
    void judgesPeriodWithInjectedClock() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW.plus(Duration.ofDays(7))));

        assertThatThrownBy(() -> service.issue(VEHICLE_ID, campaignId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CampaignErrorCode.CAMPAIGN_INACTIVE);
    }

    // 위치는 native UPDATE 로 기록되므로 캐시된 상태 엔티티를 비워 매니페스트가 DB 값을 읽게 한다
    private void checkInAt(VehicleLocation location) {
        checkInService.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.0", Optional.of(location), Optional.empty()));
        em.flush();
        em.clear();
    }

    private String assignedEdgeId() {
        em.flush();
        return (String) em.createNativeQuery(
                        "SELECT s.assigned_edge_id FROM vehicle_state s JOIN vehicle v ON v.id = s.vehicle_id"
                                + " WHERE v.external_id = :vehicleId")
                .setParameter("vehicleId", VEHICLE_ID)
                .getSingleResult();
    }

    private Long registerCampaign(String targetVersion, CampaignPeriod period) {
        ArtifactRelease release = new ArtifactRelease("model-a", targetVersion);
        Artifact artifact = new Artifact(release, new ArtifactFile(SHA256, FILE_SIZE, release.originPath()));
        em.persist(artifact);
        CampaignTarget target = new CampaignTarget("model-a", Set.of("hw-1"), Set.of("KR"));
        CampaignCondition condition = new CampaignCondition(target, VersionRange.unbounded(), period);
        Long campaignId = campaignRepository
                .save(new Campaign(new CampaignName("campaign"), artifact.getId(), condition))
                .getId();
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
