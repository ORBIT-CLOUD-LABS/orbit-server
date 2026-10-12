package com.orbit.server.domain.vehicle.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.vehicle.entity.VehicleToken;
import com.orbit.server.global.error.CommonErrorCode;
import com.orbit.server.global.security.AuthErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 등록 키 인터셉터부터 저장까지 실제 요청 흐름으로 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class VehicleControllerTest {

    private static final String URL = "/api/v1/vehicles/register";
    // application-test.yml 의 orbit.enrollment.key
    private static final String ENROLLMENT_KEY = "test-enrollment-key";
    private static final String VEHICLE_ID = "vehicle-001";
    private static final String BODY = """
            {"vehicleId": "vehicle-001", "model": "model-a", "hwVersion": "hw-1",
             "region": "KR", "currentVersion": "1.0.0"}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("VEHICLE-REQ-002: 처음 등록하면 201 과 차량 ID·토큰·발급 시각을 응답한다")
    void registersNewVehicle() throws Exception {
        register(ENROLLMENT_KEY, BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleId").value(VEHICLE_ID))
                .andExpect(jsonPath("$.vehicleToken").isNotEmpty())
                .andExpect(jsonPath("$.issuedAt").isNotEmpty());
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 응답한 토큰 원문은 저장하지 않고 해시만 유효 토큰으로 저장한다")
    void storesOnlyTokenHash() throws Exception {
        String token = tokenOf(register(ENROLLMENT_KEY, BODY));

        assertThat(activeTokenHash())
                .isEqualTo(new VehicleToken(token).hash().value())
                .isNotEqualTo(token);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 재등록하면 200 과 새 토큰을 응답하고 기존 토큰은 폐기한다")
    void reregistersAndRevokesPreviousToken() throws Exception {
        String previous = tokenOf(register(ENROLLMENT_KEY, BODY));

        String current = tokenOf(register(ENROLLMENT_KEY, BODY).andExpect(status().isOk()));

        assertThat(current).isNotEqualTo(previous);
        assertThat(activeTokenHash()).isEqualTo(new VehicleToken(current).hash().value());
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 등록 키가 틀리면 401 INVALID_ENROLLMENT_KEY 이고 차량을 등록하지 않는다")
    void rejectsInvalidEnrollmentKey() throws Exception {
        register("wrong-key", BODY)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ENROLLMENT_KEY.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vehicle", Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 필수 값이 없으면 400 INVALID_INPUT 으로 응답한다")
    void rejectsMissingField() throws Exception {
        String body = """
                {"vehicleId": "vehicle-001", "model": "model-a", "hwVersion": "hw-1", "region": "KR"}""";

        register(ENROLLMENT_KEY, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));
    }

    private ResultActions register(String enrollmentKey, String body) throws Exception {
        return mockMvc.perform(post(URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + enrollmentKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String tokenOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.vehicleToken");
    }

    private String activeTokenHash() {
        return jdbcTemplate.queryForObject(
                "SELECT c.token_hash FROM vehicle_credential c JOIN vehicle v ON v.id = c.vehicle_id"
                        + " WHERE v.external_id = ? AND c.status = 'ACTIVE'",
                String.class,
                VEHICLE_ID);
    }
}
