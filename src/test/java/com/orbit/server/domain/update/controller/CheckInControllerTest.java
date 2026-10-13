package com.orbit.server.domain.update.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.error.CommonErrorCode;
import com.orbit.server.global.security.AuthErrorCode;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

/** 차량 등록으로 받은 토큰으로 인터셉터부터 대상 판정까지 실제 요청 흐름으로 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CheckInControllerTest {

    private static final String URL = "/api/v1/vehicles/{vehicleId}/check-in";
    // application-test.yml 의 orbit.enrollment.key
    private static final String ENROLLMENT_KEY = "test-enrollment-key";
    private static final String VEHICLE_ID = "vehicle-001";

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
    @DisplayName("VEHICLE-REQ-001: 조건이 맞는 캠페인이 있으면 대상과 캠페인 ID 를 응답한다")
    void respondsTargetCampaign() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1");

        checkIn(VEHICLE_ID, token, "1.0.0")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updateTarget").value(true))
                .andExpect(jsonPath("$.campaignId").value(campaignId))
                .andExpect(jsonPath("$.nextCheckInSeconds").value(30));
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 여러 캠페인이 맞으면 목표 버전이 숫자로 가장 높은 1.0.10 을 고른다")
    void respondsHighestTargetVersion() throws Exception {
        registerCampaign("1.0.9", "hw-1");
        Long higher = registerCampaign("1.0.10", "hw-1");

        checkIn(VEHICLE_ID, token, "1.0.0").andExpect(jsonPath("$.campaignId").value(higher));
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 활성 캠페인이 없으면 대상이 아니고 캠페인 ID 를 빼고 응답한다")
    void respondsNotTargetWithoutCampaign() throws Exception {
        checkIn(VEHICLE_ID, token, "1.0.0")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updateTarget").value(false))
                .andExpect(jsonPath("$.campaignId").doesNotExist())
                .andExpect(jsonPath("$.nextCheckInSeconds").value(30));
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 대상 조건이 맞지 않으면 대상이 아니다")
    void respondsNotTargetWhenConditionMismatches() throws Exception {
        registerCampaign("1.0.1", "hw-2");

        checkIn(VEHICLE_ID, token, "1.0.0").andExpect(jsonPath("$.updateTarget").value(false));
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 현재 버전이 이미 목표 버전이면 대상이 아니다")
    void respondsNotTargetWhenAlreadyOnTargetVersion() throws Exception {
        registerCampaign("1.0.1", "hw-1");

        checkIn(VEHICLE_ID, token, "1.0.1").andExpect(jsonPath("$.updateTarget").value(false));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 토큰이 없으면 401 UNAUTHORIZED 로 응답한다")
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(post(URL, VEHICLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf("1.0.0")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 재등록으로 폐기한 토큰이면 401 UNAUTHORIZED 로 응답한다")
    void rejectsRevokedToken() throws Exception {
        String revoked = token;
        register(VEHICLE_ID);

        checkIn(VEHICLE_ID, revoked, "1.0.0")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 다른 차량의 토큰이면 403 VEHICLE_MISMATCH 로 응답한다")
    void rejectsTokenOfOtherVehicle() throws Exception {
        String otherToken = register("vehicle-002");

        checkIn(VEHICLE_ID, otherToken, "1.0.0")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(VehicleErrorCode.VEHICLE_MISMATCH.getCode()));
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 현재 버전이 없으면 400 INVALID_INPUT 으로 응답한다")
    void rejectsMissingCurrentVersion() throws Exception {
        mockMvc.perform(post(URL, VEHICLE_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 성공 보고를 기록하고 체크인에 응답한다")
    void recordsSucceededReport() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1");

        checkInWithLastUpdate("1.0.1", lastUpdateOf(campaignId, "SUCCEEDED", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updateTarget").value(false));

        assertThat(updateResults()).containsExactly(campaignId + ":SUCCEEDED:null");
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 실패 보고를 실패 사유와 함께 기록한다")
    void recordsFailedReport() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1");

        checkInWithLastUpdate("1.0.0", lastUpdateOf(campaignId, "FAILED", "\"HASH_MISMATCH\""))
                .andExpect(status().isOk());

        assertThat(updateResults()).containsExactly(campaignId + ":FAILED:HASH_MISMATCH");
    }

    @Test
    @DisplayName("UPDATE-POL-001: 응답을 못 받아 같은 결과를 다시 보내도 결과는 하나다")
    void recordsDuplicateReportOnce() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1");
        String lastUpdate = lastUpdateOf(campaignId, "FAILED", "\"DOWNLOAD_FAILED\"");

        checkInWithLastUpdate("1.0.0", lastUpdate).andExpect(status().isOk());
        checkInWithLastUpdate("1.0.0", lastUpdate).andExpect(status().isOk());

        assertThat(updateResults()).containsExactly(campaignId + ":FAILED:DOWNLOAD_FAILED");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // 정의하지 않은 실패 사유
                """
                {"campaignId": %d, "result": "FAILED", "failureReason": "DISK_FULL", "finishedAt": "2026-10-01T12:00:00Z"}""",
                // 정의하지 않은 결과
                """
                {"campaignId": %d, "result": "SUCCESS", "finishedAt": "2026-10-01T12:00:00Z"}""",
                // 성공 보고에 실패 상세
                """
                {"campaignId": %d, "result": "SUCCEEDED", "failureDetail": "ok", "finishedAt": "2026-10-01T12:00:00Z"}""",
                // ISO-8601 이 아닌 완료 시각
                """
                {"campaignId": %d, "result": "SUCCEEDED", "finishedAt": "2026-10-01"}""",
                // 완료 시각 누락
                """
                {"campaignId": %d, "result": "SUCCEEDED"}"""
            })
    @DisplayName("UPDATE-POL-001: 형식이 틀린 결과는 차량이 체크인을 계속할 수 있도록 버리고 200 으로 응답한다")
    void discardsMalformedLastUpdate(String lastUpdateFormat) throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1");

        checkInWithLastUpdate("1.0.0", lastUpdateFormat.formatted(campaignId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updateTarget").value(true));

        assertThat(updateResults()).isEmpty();
    }

    @Test
    @DisplayName("UPDATE-POL-001: 캠페인 ID 가 없는 결과도 버리고 200 으로 응답한다")
    void discardsLastUpdateWithoutCampaignId() throws Exception {
        checkInWithLastUpdate("1.0.0", """
                        {"result": "SUCCEEDED", "finishedAt": "2026-10-01T12:00:00Z"}""").andExpect(status().isOk());

        assertThat(updateResults()).isEmpty();
    }

    @Test
    @DisplayName("UPDATE-POL-001: 성공 보고에 실패 사유가 있으면 결과는 버리고 체크인에 200 으로 응답한다")
    void discardsSucceededWithFailureReason() throws Exception {
        Long campaignId = registerCampaign("1.0.1", "hw-1");

        checkInWithLastUpdate("1.0.1", lastUpdateOf(campaignId, "SUCCEEDED", "\"INSTALL_FAILED\""))
                .andExpect(status().isOk());

        assertThat(updateResults()).isEmpty();
    }

    @Test
    @DisplayName("UPDATE-REQ-001: 없는 캠페인의 결과는 버리고 체크인에 200 으로 응답한다")
    void discardsResultOfMissingCampaign() throws Exception {
        checkInWithLastUpdate("1.0.1", lastUpdateOf(Long.MAX_VALUE, "SUCCEEDED", null))
                .andExpect(status().isOk());

        assertThat(updateResults()).isEmpty();
    }

    private ResultActions checkIn(String vehicleId, String vehicleToken, String currentVersion) throws Exception {
        return mockMvc.perform(post(URL, vehicleId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + vehicleToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyOf(currentVersion)));
    }

    private ResultActions checkInWithLastUpdate(String currentVersion, String lastUpdate) throws Exception {
        String body = """
                {"currentVersion": "%s", "lastUpdate": %s}""".formatted(currentVersion, lastUpdate);
        return mockMvc.perform(post(URL, VEHICLE_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // failureReason 은 JSON 값 그대로 받는다 (null 또는 따옴표로 감싼 문자열)
    private static String lastUpdateOf(Long campaignId, String result, String failureReason) {
        return """
                {"campaignId": %d, "result": "%s", "failureReason": %s, "finishedAt": "2026-10-01T12:00:00Z"}""".formatted(campaignId, result, failureReason);
    }

    // 엔티티에 getter 를 두지 않으므로 기록한 결과를 DB 에서 직접 읽는다
    @SuppressWarnings("unchecked")
    private List<String> updateResults() {
        em.flush();
        return em.createNativeQuery(
                        "SELECT CONCAT_WS(':', r.campaign_id, r.result, IFNULL(r.failure_reason, 'null'))"
                                + " FROM update_result r JOIN vehicle v ON v.id = r.vehicle_id WHERE v.external_id = :vehicleId")
                .setParameter("vehicleId", VEHICLE_ID)
                .getResultList();
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
        return JsonPath.read(response, "$.vehicleToken");
    }

    // 실제 시계로 판정하므로 지금을 포함하는 기간으로 등록한다
    private Long registerCampaign(String targetVersion, String hwVersion) {
        ArtifactRelease release = new ArtifactRelease("model-a", targetVersion);
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        Instant now = Instant.now();
        CampaignPeriod period = new CampaignPeriod(now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(1)));
        CampaignTarget target = new CampaignTarget("model-a", Set.of(hwVersion), Set.of("KR"));
        CampaignCondition condition = new CampaignCondition(target, VersionRange.unbounded(), period);
        Long campaignId = campaignRepository
                .save(new Campaign(new CampaignName("campaign"), artifact.id(), condition))
                .id();
        em.flush();
        em.clear();
        return campaignId;
    }

    private static String bodyOf(String currentVersion) {
        return """
                {"currentVersion": "%s"}""".formatted(currentVersion);
    }
}
