package com.orbit.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.global.error.CommonErrorCode;
import jakarta.servlet.DispatcherType;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
    private static final String SNAPSHOT_URL = "/api/v1/dashboard/vehicles";
    private static final String VEHICLE_ID = "vehicle-001";
    private static final String REGISTER_URL = "/api/v1/vehicles/register";
    private static final String CHECK_IN_URL = "/api/v1/vehicles/" + VEHICLE_ID + "/check-in";
    private static final String UNKNOWN_VEHICLE_URL = "/api/v1/vehicles/" + VEHICLE_ID + "/unknown";
    private static final String VEHICLES_ROOT_URL = "/api/v1/vehicles";
    private static final String REGISTER_BODY = """
            {"vehicleId": "%s", "model": "model-a", "hwVersion": "hw-1", "region": "KR", "currentVersion": "1.0.0"}""".formatted(VEHICLE_ID);
    private static final String UNKNOWN_URL = "/api/v1/unknown";
    private static final String PROMETHEUS_URL = "/actuator/prometheus";

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

    // SSE 처럼 비동기로 처리한 요청은 응답을 마칠 때 같은 요청이 ASYNC 로 다시 dispatch 된다.
    // 관리자 토큰 필터는 ASYNC dispatch 를 건너뛰므로, 막히면 이미 응답 중인 스트림에 오류를 쓰려다 실패한다
    @Test
    @DisplayName("인가를 통과한 대시보드 요청의 ASYNC dispatch 는 다시 막지 않는다")
    void allowsAsyncDispatchOfDashboardRequest() throws Exception {
        mockMvc.perform(get(SNAPSHOT_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_TOKEN))
                        .with(request -> {
                            request.setDispatcherType(DispatcherType.ASYNC);
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
    @DisplayName("health 는 인증 없이 응답한다")
    void allowsHealthWithoutToken(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    // 수집기는 토큰 없이 주기적으로 긁어 가므로 health 와 같은 수준으로 열고, 접근은 네트워크 경계에서 막는다
    @Test
    @DisplayName("Prometheus 메트릭은 인증 없이 Prometheus 형식으로 응답한다")
    void allowsPrometheusWithoutToken() throws Exception {
        mockMvc.perform(get(PROMETHEUS_URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }

    // 메트릭만 열고, 설정값·환경 변수를 보여 주는 다른 actuator 엔드포인트는 계속 막혀 있어야 한다
    @ParameterizedTest
    @ValueSource(strings = {"/actuator", "/actuator/env", "/actuator/metrics"})
    @DisplayName("Prometheus 외의 actuator 엔드포인트는 인증 없이 열지 않는다")
    void rejectsOtherActuatorEndpointsWithoutToken(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.AUTHENTICATION_REQUIRED.getCode()));
    }

    // test 프로필은 springdoc 을 켜지 않으므로, 인증을 통과해 MVC 까지 가서 404 가 나는지로 확인한다
    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html", "/swagger-ui.html"})
    @DisplayName("API 문서 경로는 인증 대상이 아니고, springdoc 이 꺼져 있으면 404 로 응답한다")
    void hidesApiDocsWhenDisabled(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    // 인가 단계에서 막히면 AUTHENTICATION_REQUIRED 가 나므로, 차량 토큰 필터의 오류 코드로 그 필터가 거부했는지 확인한다
    @Test
    @DisplayName("차량 토큰이 없으면 차량 API 에 401 UNAUTHORIZED 로 응답한다")
    void rejectsVehicleApiWithoutToken() throws Exception {
        mockMvc.perform(post(CHECK_IN_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
    }

    @ParameterizedTest
    @ValueSource(strings = {ADMIN_TOKEN, ENROLLMENT_KEY})
    @DisplayName("관리자 토큰과 등록 키는 차량 API 에서 401 UNAUTHORIZED 로 거부한다")
    void rejectsVehicleApiWithOtherCredential(String credential) throws Exception {
        mockMvc.perform(post(CHECK_IN_URL).header(HttpHeaders.AUTHORIZATION, bearer(credential)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
    }

    // 인증이 요청 매핑보다 먼저라, 토큰이 없으면 메서드가 틀린 요청도 405 가 아니라 인증 실패로 끝난다
    @Test
    @DisplayName("차량 토큰이 없으면 메서드가 틀린 차량 API 요청에도 401 UNAUTHORIZED 로 응답한다")
    void rejectsVehicleApiWithWrongMethodWithoutToken() throws Exception {
        mockMvc.perform(get(CHECK_IN_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @Transactional
    @DisplayName("차량 토큰이 맞으면 메서드가 틀린 차량 API 요청에 405 METHOD_NOT_ALLOWED 로 응답한다")
    void respondsMethodNotAllowedWithValidToken() throws Exception {
        String vehicleToken = registerVehicle();

        mockMvc.perform(get(CHECK_IN_URL).header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.METHOD_NOT_ALLOWED.getCode()));
    }

    @Test
    @Transactional
    @DisplayName("등록 키가 맞으면 차량 등록 API 를 처리하고 차량 토큰을 발급한다")
    void allowsRegisterWithValidKey() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ENROLLMENT_KEY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.vehicleToken").isNotEmpty());
    }

    @Test
    @DisplayName("등록 키가 없으면 차량 등록 API 에 401 INVALID_ENROLLMENT_KEY 로 응답한다")
    void rejectsRegisterWithoutKey() throws Exception {
        mockMvc.perform(post(REGISTER_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ENROLLMENT_KEY.getCode()));
    }

    @Test
    @DisplayName("관리자 토큰은 차량 등록 API 에서 401 INVALID_ENROLLMENT_KEY 로 거부한다")
    void rejectsRegisterWithAdminToken() throws Exception {
        mockMvc.perform(post(REGISTER_URL).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ENROLLMENT_KEY.getCode()));
    }

    @Test
    @Transactional
    @DisplayName("차량 토큰은 차량 등록 API 에서 401 INVALID_ENROLLMENT_KEY 로 거부한다")
    void rejectsRegisterWithVehicleToken() throws Exception {
        String vehicleToken = registerVehicle();

        mockMvc.perform(post(REGISTER_URL).header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ENROLLMENT_KEY.getCode()));
    }

    @Test
    @Transactional
    @DisplayName("차량 토큰은 관리자 API 에서 401 AUTHENTICATION_REQUIRED 로 거부한다")
    void rejectsAdminApiWithVehicleToken() throws Exception {
        String vehicleToken = registerVehicle();

        mockMvc.perform(get(ADMIN_URL).header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.AUTHENTICATION_REQUIRED.getCode()));
    }

    // 차량은 UNAUTHORIZED 를 재등록 신호로 쓰므로, 등록 URL 을 잘못 쓴 요청에 차량 토큰용 코드가 나가면 재등록을 반복한다.
    // register 가 vehicleId 로 해석되지 않고 등록 키 인증을 거치는지 확인한다
    @ParameterizedTest
    @ValueSource(strings = {REGISTER_URL + "/", REGISTER_URL + "/extra"})
    @DisplayName("등록 경로에 끝 슬래시나 하위 경로가 붙어도 등록 키가 없으면 401 INVALID_ENROLLMENT_KEY 로 응답한다")
    void rejectsRegisterSubPathWithoutKey(String url) throws Exception {
        mockMvc.perform(post(url))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ENROLLMENT_KEY.getCode()));
    }

    @ParameterizedTest
    @Transactional
    @ValueSource(strings = {REGISTER_URL + "/", REGISTER_URL + "/extra"})
    @DisplayName("등록 경로에 끝 슬래시나 하위 경로가 붙으면 차량 토큰이 맞아도 401 INVALID_ENROLLMENT_KEY 로 응답한다")
    void rejectsRegisterSubPathWithVehicleToken(String url) throws Exception {
        String vehicleToken = registerVehicle();

        mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ENROLLMENT_KEY.getCode()));
    }

    // 등록 API 는 정확한 경로에만 있으므로, 등록 키 인증을 통과해 MVC 까지 가서 404 가 나는지로 확인한다
    @ParameterizedTest
    @ValueSource(strings = {REGISTER_URL + "/", REGISTER_URL + "/extra"})
    @DisplayName("등록 경로에 끝 슬래시나 하위 경로가 붙으면 등록 키가 맞아도 404 로 응답한다")
    void respondsNotFoundForRegisterSubPathWithValidKey(String url) throws Exception {
        mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, bearer(ENROLLMENT_KEY)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    // 컨트롤러가 없는 차량 경로로, 인증을 통과해 MVC 까지 가서 404 가 나는지로 확인한다
    @Test
    @Transactional
    @DisplayName("차량 토큰이 맞으면 차량 경로의 인증을 통과한다")
    void allowsVehiclePathWithValidToken() throws Exception {
        String vehicleToken = registerVehicle();

        mockMvc.perform(get(UNKNOWN_VEHICLE_URL).header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("컨트롤러가 없는 차량 경로도 차량 토큰이 없으면 401 UNAUTHORIZED 로 응답한다")
    void rejectsUnknownVehiclePathWithoutToken() throws Exception {
        mockMvc.perform(get(UNKNOWN_VEHICLE_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
    }

    // 차량 토큰 필터가 걸리지 않는 경로라 필터가 인증하지 않는다. 인가 규칙이 차량 권한을 요구해 열리지 않는지 확인한다
    @Test
    @DisplayName("vehicleId 가 없는 차량 경로는 401 AUTHENTICATION_REQUIRED 로 거부한다")
    void rejectsVehiclePathWithoutVehicleId() throws Exception {
        mockMvc.perform(get(VEHICLES_ROOT_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.AUTHENTICATION_REQUIRED.getCode()));
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

    // 차량 토큰은 등록으로만 발급되므로 실제 등록 요청으로 받는다. 호출한 테스트의 트랜잭션과 함께 롤백된다
    private String registerVehicle() throws Exception {
        String response = mockMvc.perform(post(REGISTER_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ENROLLMENT_KEY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.vehicleToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
