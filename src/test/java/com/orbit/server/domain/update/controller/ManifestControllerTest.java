package com.orbit.server.domain.update.controller;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.common.SuccessCode;
import com.orbit.server.global.error.CommonErrorCode;
import com.orbit.server.global.security.AuthErrorCode;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 차량 등록으로 받은 토큰으로 인터셉터부터 매니페스트 발급까지 실제 요청 흐름으로 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ManifestControllerTest {

    private static final String URL = "/api/v1/vehicles/{vehicleId}/campaigns/{campaignId}/manifest";
    // application-test.yml 의 orbit.enrollment.key
    private static final String ENROLLMENT_KEY = "test-enrollment-key";
    private static final String VEHICLE_ID = "vehicle-001";
    private static final String SHA256 = "a".repeat(64);
    private static final long FILE_SIZE = 1024;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private EntityManager em;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = register(VEHICLE_ID);
    }

    @Test
    @DisplayName("MANIFEST-REQ-001: 대상 차량에 캠페인 ID·목표 버전·파일 크기·해시·다운로드 URL 을 응답한다")
    void respondsManifest() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1", periodAroundNow());

        requestManifest(VEHICLE_ID, campaignId, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(SuccessCode.OK.getCode()))
                .andExpect(jsonPath("$.data.campaignId").value(campaignId))
                .andExpect(jsonPath("$.data.targetVersion").value("1.0.1"))
                .andExpect(jsonPath("$.data.fileSize").value(FILE_SIZE))
                .andExpect(jsonPath("$.data.sha256").value(SHA256));
    }

    @Test
    @DisplayName("DOWNLOAD-POL-001: 다운로드 URL 은 고른 Edge 주소 뒤에 Origin 경로와 URL 서명(md5, expires, kid)을 붙인 URL 이다")
    void respondsSignedEdgeDownloadUrl() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1", periodAroundNow());

        // 위치를 보낸 적이 없으므로 application-test.yml 의 기본 Edge(edge-seoul) 주소다
        requestManifest(VEHICLE_ID, campaignId, token)
                .andExpect(jsonPath("$.data.downloadUrl").value(signedUrlOf("http://cdn.test:30081")))
                .andExpect(jsonPath("$.data.urlExpiresAt").doesNotExist());
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 위치를 모르면 기본 Edge 를 거리 없이 응답하고 나머지 Edge 를 설정 순서대로 예비 URL 로 준다")
    void respondsDefaultEdgeWithFallbacksWithoutLocation() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1", periodAroundNow());

        requestManifest(VEHICLE_ID, campaignId, token)
                .andExpect(jsonPath("$.data.edge.edgeId").value("edge-seoul"))
                .andExpect(jsonPath("$.data.edge.distanceKm").value(nullValue()))
                .andExpect(jsonPath("$.data.fallbackUrls.length()").value(2))
                .andExpect(jsonPath("$.data.fallbackUrls[0].edgeId").value("edge-daejeon"))
                .andExpect(jsonPath("$.data.fallbackUrls[0].url").value(signedUrlOf("http://cdn.test:30082")))
                .andExpect(jsonPath("$.data.fallbackUrls[1].edgeId").value("edge-busan"));
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 체크인으로 보낸 위치에서 가장 가까운 정상 Edge 로 URL 을 만들고 거리를 함께 응답한다")
    void respondsNearestEdgeForCheckInLocation() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1", periodAroundNow());
        // 울산 근처: application-test.yml 의 Edge 중 부산이 가장 가깝다
        checkInAt(35.5384, 129.3114);

        requestManifest(VEHICLE_ID, campaignId, token)
                .andExpect(jsonPath("$.data.edge.edgeId").value("edge-busan"))
                .andExpect(jsonPath("$.data.edge.distanceKm").value(closeTo(50.0, 5.0)))
                .andExpect(jsonPath("$.data.downloadUrl").value(signedUrlOf("http://cdn.test:30083")))
                .andExpect(jsonPath("$.data.fallbackUrls[0].edgeId").value("edge-daejeon"))
                .andExpect(jsonPath("$.data.fallbackUrls[1].edgeId").value("edge-seoul"));
    }

    @Test
    @DisplayName("MANIFEST-REQ-001: 캠페인이 없으면 404 CAMPAIGN_NOT_FOUND 로 응답한다")
    void rejectsMissingCampaign() throws Exception {
        requestManifest(VEHICLE_ID, Long.MAX_VALUE, token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CampaignErrorCode.CAMPAIGN_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 비활성 캠페인이면 410 CAMPAIGN_INACTIVE 로 응답한다")
    void rejectsInactiveCampaign() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1", periodAroundNow());
        deactivate(campaignId);

        requestManifest(VEHICLE_ID, campaignId, token)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(CampaignErrorCode.CAMPAIGN_INACTIVE.getCode()));
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 적용 기간 밖이면 410 CAMPAIGN_INACTIVE 로 응답한다")
    void rejectsCampaignOutOfPeriod() throws Exception {
        Instant later = Instant.now().plus(Duration.ofDays(7));
        Long campaignId = registerCampaign("1.0.1", "hw-1", new CampaignPeriod(later, later.plus(Duration.ofDays(1))));

        requestManifest(VEHICLE_ID, campaignId, token)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(CampaignErrorCode.CAMPAIGN_INACTIVE.getCode()));
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 대상 조건이 맞지 않으면 409 NOT_UPDATE_TARGET 으로 응답한다")
    void rejectsWhenConditionMismatches() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-2", periodAroundNow());

        requestManifest(VEHICLE_ID, campaignId, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(CampaignErrorCode.NOT_UPDATE_TARGET.getCode()));
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 현재 버전이 이미 목표 버전이면 409 NOT_UPDATE_TARGET 으로 응답한다")
    void rejectsWhenAlreadyOnTargetVersion() throws Exception {
        // 등록 때 보낸 현재 버전이 1.0.0 이다
        Long campaignId = registerCampaign("1.0.0", "hw-1", periodAroundNow());

        requestManifest(VEHICLE_ID, campaignId, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(CampaignErrorCode.NOT_UPDATE_TARGET.getCode()));
    }

    @Test
    @DisplayName("MANIFEST-REQ-001: 캠페인 ID 가 숫자가 아니면 400 INVALID_INPUT 으로 응답한다")
    void rejectsNonNumericCampaignId() throws Exception {
        mockMvc.perform(get(URL, VEHICLE_ID, "cmp-001").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 토큰이 없으면 401 UNAUTHORIZED 로 응답한다")
    void rejectsMissingToken() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1", periodAroundNow());

        mockMvc.perform(get(URL, VEHICLE_ID, campaignId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 다른 차량의 토큰이면 403 VEHICLE_MISMATCH 로 응답한다")
    void rejectsTokenOfOtherVehicle() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1", periodAroundNow());
        String otherToken = register("vehicle-002");

        requestManifest(VEHICLE_ID, campaignId, otherToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(VehicleErrorCode.VEHICLE_MISMATCH.getCode()));
    }

    private ResultActions requestManifest(String vehicleId, Long campaignId, String vehicleToken) throws Exception {
        return mockMvc.perform(
                get(URL, vehicleId, campaignId).header(HttpHeaders.AUTHORIZATION, "Bearer " + vehicleToken));
    }

    private String register(String vehicleId) throws Exception {
        String body = """
                {"vehicleId": "%s", "model": "model-a", "hwVersion": "hw-1", "region": "KR", "currentVersion": "1.0.0"}""".formatted(vehicleId);
        String response = mockMvc.perform(post("/api/v1/vehicles/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ENROLLMENT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.vehicleToken");
    }

    // 위치는 native UPDATE 로 기록되므로 캐시된 상태 엔티티를 비워 매니페스트가 DB 값을 읽게 한다
    private void checkInAt(double latitude, double longitude) throws Exception {
        String body = """
                {"currentVersion": "1.0.0", "location": {"latitude": %s, "longitude": %s}}""".formatted(latitude, longitude);
        mockMvc.perform(post("/api/v1/vehicles/{vehicleId}/check-in", VEHICLE_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        em.flush();
        em.clear();
    }

    private Long registerCampaign(String targetVersion, String hwVersion, CampaignPeriod period) {
        ArtifactRelease release = new ArtifactRelease("model-a", targetVersion);
        Artifact artifact = new Artifact(release, new ArtifactFile(SHA256, FILE_SIZE, release.originPath()));
        em.persist(artifact);
        CampaignTarget target = new CampaignTarget("model-a", Set.of(hwVersion), Set.of("KR"));
        CampaignCondition condition = new CampaignCondition(target, VersionRange.unbounded(), period);
        Long campaignId = campaignRepository
                .save(new Campaign(new CampaignName("campaign"), artifact.getId(), condition))
                .getId();
        em.flush();
        em.clear();
        return campaignId;
    }

    // 비활성화 기능은 아직 없으므로 DB 상태만 바꾼다
    private void deactivate(Long campaignId) {
        em.createNativeQuery("UPDATE campaign SET status = 'INACTIVE' WHERE id = :id")
                .setParameter("id", campaignId)
                .executeUpdate();
    }

    // 실제 시계로 판정하므로 지금을 포함하는 기간으로 등록한다
    // 만료 시각이 요청 시각에 따라 달라 서명 값을 고정할 수 없으므로 형식만 본다. 서명 값은 EdgeDownloadUrlIssuerTest 가 검증한다
    private static Matcher<String> signedUrlOf(String edgeBaseUrl) {
        return matchesPattern(Pattern.quote(edgeBaseUrl + "/firmware/model-a/1.0.1/firmware.bin")
                + "\\?md5=[A-Za-z0-9_-]{22}&expires=\\d+&kid=test");
    }

    private static CampaignPeriod periodAroundNow() {
        Instant now = Instant.now();
        return new CampaignPeriod(now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(1)));
    }
}
