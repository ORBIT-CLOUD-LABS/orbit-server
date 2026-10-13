package com.orbit.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.global.error.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 보안 필터 체인부터 컨트롤러까지 실제 요청 흐름으로 경로별 인증 규칙을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class SecurityConfigTest {

    // application-test.yml 의 orbit.admin.token, orbit.enrollment.key
    private static final String ADMIN_TOKEN = "test-admin-token";
    private static final String ENROLLMENT_KEY = "test-enrollment-key";
    private static final String WRONG_TOKEN = "wrong-admin-token";

    private static final String ADMIN_URL = "/api/v1/admin/artifacts";
    private static final String DASHBOARD_URL = "/api/v1/dashboard/unknown";
    private static final String CHECK_IN_URL = "/api/v1/vehicles/vehicle-001/check-in";
    private static final String UNKNOWN_URL = "/api/v1/unknown";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("관리자 토큰이 맞으면 관리자 API 를 처리한다")
    void allowsAdminApiWithValidToken() throws Exception {
        mockMvc.perform(get(ADMIN_URL).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 토큰이 없으면 관리자 API 에 401 AUTHENTICATION_REQUIRED 로 응답한다")
    void rejectsAdminApiWithoutToken() throws Exception {
        mockMvc.perform(get(ADMIN_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.AUTHENTICATION_REQUIRED.getCode()))
                .andExpect(jsonPath("$.message").value(CommonErrorCode.AUTHENTICATION_REQUIRED.getMessage()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer " + WRONG_TOKEN, "Bearer " + ENROLLMENT_KEY, ADMIN_TOKEN, "Basic " + ADMIN_TOKEN})
    @DisplayName("관리자 토큰이 틀리거나 Bearer 형식이 아니면 401 AUTHENTICATION_REQUIRED 로 응답한다")
    void rejectsAdminApiWithInvalidToken(String authorization) throws Exception {
        mockMvc.perform(get(ADMIN_URL).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.AUTHENTICATION_REQUIRED.getCode()));
    }

    @Test
    @DisplayName("틀린 토큰 값을 오류 응답과 로그에 남기지 않는다")
    void doesNotExposeToken(CapturedOutput output) throws Exception {
        MvcResult result = mockMvc.perform(get(ADMIN_URL).header(HttpHeaders.AUTHORIZATION, bearer(WRONG_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(WRONG_TOKEN);
        assertThat(output.getAll()).doesNotContain(WRONG_TOKEN);
    }

    @Test
    @DisplayName("관리자 토큰이 없으면 대시보드 API 에 401 AUTHENTICATION_REQUIRED 로 응답한다")
    void rejectsDashboardApiWithoutToken() throws Exception {
        mockMvc.perform(get(DASHBOARD_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.AUTHENTICATION_REQUIRED.getCode()));
    }

    // 대시보드 API 는 아직 없으므로, 인증을 통과해 MVC 까지 가서 404 가 나는지로 확인한다
    @Test
    @DisplayName("관리자 토큰이 맞으면 대시보드 경로의 인증을 통과한다")
    void allowsDashboardPathWithValidToken() throws Exception {
        mockMvc.perform(get(DASHBOARD_URL).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_TOKEN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
    @DisplayName("health 는 인증 없이 응답한다")
    void allowsHealthWithoutToken(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    // 관리자 인증 대상이면 AUTHENTICATION_REQUIRED 가 나므로, 차량 토큰 인터셉터의 오류 코드로 필터를 통과했는지 확인한다
    @Test
    @DisplayName("차량 API 는 관리자 인증 대상이 아니고 차량 토큰 인증을 거친다")
    void leavesVehicleApiToVehicleAuthentication() throws Exception {
        mockMvc.perform(post(CHECK_IN_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("규칙에 없는 경로는 401 AUTHENTICATION_REQUIRED 로 거부한다")
    void rejectsUnknownPath() throws Exception {
        mockMvc.perform(get(UNKNOWN_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.AUTHENTICATION_REQUIRED.getCode()));
    }

    @Test
    @DisplayName("관리자 토큰이 있어도 규칙에 없는 경로는 403 ACCESS_DENIED 로 거부한다")
    void rejectsUnknownPathEvenForAdmin() throws Exception {
        mockMvc.perform(get(UNKNOWN_URL).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.message").value(CommonErrorCode.ACCESS_DENIED.getMessage()));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
