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
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.service.RegisterVehicleCommand;
import com.orbit.server.domain.vehicle.service.VehicleRegistrationService;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
@Import({TestcontainersConfiguration.class, CheckInServiceTest.FixedClockConfig.class})
@Transactional
class CheckInServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final String VEHICLE_ID = "vehicle-001";

    @Autowired
    private CheckInService service;

    @Autowired
    private VehicleRegistrationService registrationService;

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
    @DisplayName("VEHICLE-REQ-001: 대상 캠페인이 있으면 캠페인 ID 와 체크인 주기를 알려 준다")
    void returnsTargetCampaign() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW));

        CheckInResult result = service.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.0"));

        assertThat(result.updateTarget()).isTrue();
        assertThat(result.targetCampaignId()).contains(campaignId);
        assertThat(result.nextCheckInSeconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 활성 캠페인이 없으면 대상이 아니다")
    void returnsNotTargetWithoutCampaign() {
        CheckInResult result = service.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.0"));

        assertThat(result.updateTarget()).isFalse();
        assertThat(result.nextCheckInSeconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 주입한 시계의 시각으로 적용 기간을 판정한다")
    void judgesPeriodWithInjectedClock() {
        registerCampaign("1.0.1", periodAround(NOW.plus(Duration.ofDays(7))));

        CheckInResult result = service.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.0"));

        assertThat(result.updateTarget()).isFalse();
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 체크인한 현재 버전과 마지막 체크인 시각을 기록한다")
    void recordsCheckIn() {
        service.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.1"));

        assertThat(stateOf()).containsExactly("1.0.1:true");
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 현재 버전이 비어 있으면 거부한다")
    void rejectsBlankVersion() {
        assertThatThrownBy(() -> service.checkIn(new CheckInCommand(VEHICLE_ID, " ")))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 업데이트 결과가 있으면 상태와 함께 기록한다")
    void recordsLastUpdateWithState() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW));
        ReportUpdateResultCommand report =
                new ReportUpdateResultCommand(campaignId, "SUCCEEDED", null, null, NOW.toString());

        CheckInResult result = service.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.1", Optional.of(report)));

        assertThat(updateResultsOf(campaignId)).containsExactly("SUCCEEDED");
        assertThat(stateOf()).containsExactly("1.0.1:true");
        assertThat(result.updateTarget()).isFalse();
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 업데이트 결과가 없으면 결과를 기록하지 않는다")
    void recordsNoResultWithoutLastUpdate() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW));

        service.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.0"));

        assertThat(updateResultsOf(campaignId)).isEmpty();
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 없는 캠페인의 결과가 와도 체크인은 처리한다")
    void checksInWithResultOfMissingCampaign() {
        ReportUpdateResultCommand report =
                new ReportUpdateResultCommand(Long.MAX_VALUE, "SUCCEEDED", null, null, NOW.toString());

        CheckInResult result = service.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.1", Optional.of(report)));

        assertThat(result.nextCheckInSeconds()).isEqualTo(30);
        assertThat(stateOf()).containsExactly("1.0.1:true");
    }

    @Test
    @DisplayName("UPDATE-POL-001: 업데이트 결과의 형식이 틀리면 체크인을 거부한다")
    void rejectsMalformedLastUpdate() {
        Long campaignId = registerCampaign("1.0.1", periodAround(NOW));
        ReportUpdateResultCommand report =
                new ReportUpdateResultCommand(campaignId, "FAILED", null, null, NOW.toString());

        assertThatThrownBy(() -> service.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.0", Optional.of(report))))
                .isInstanceOf(InvalidValueException.class);
    }

    // 엔티티에 getter 를 두지 않으므로 기록한 결과를 DB 에서 직접 읽는다
    @SuppressWarnings("unchecked")
    private List<String> updateResultsOf(Long campaignId) {
        em.flush();
        return em.createNativeQuery("SELECT result FROM update_result WHERE campaign_id = :campaignId")
                .setParameter("campaignId", campaignId)
                .getResultList();
    }

    private Long registerCampaign(String targetVersion, CampaignPeriod period) {
        ArtifactRelease release = new ArtifactRelease("model-a", targetVersion);
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
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

    // 엔티티에 getter 를 두지 않으므로 저장된 상태를 DB 에서 직접 읽는다
    @SuppressWarnings("unchecked")
    private List<String> stateOf() {
        em.flush();
        return em.createNativeQuery(
                        "SELECT CONCAT(s.current_version, ':', IF(s.last_seen_at IS NULL, 'false', 'true'))"
                                + " FROM vehicle_state s JOIN vehicle v ON v.id = s.vehicle_id WHERE v.external_id = :vehicleId")
                .setParameter("vehicleId", VEHICLE_ID)
                .getResultList();
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
