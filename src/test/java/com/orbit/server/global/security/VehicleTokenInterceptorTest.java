package com.orbit.server.global.security;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
        controllers = VehicleTokenInterceptorTest.ProbeController.class,
        properties = "orbit.enrollment.key=test-enrollment-key")
@Import({VehicleTokenInterceptorTest.ProbeController.class, GlobalExceptionHandler.class, SecurityConfig.class})
class VehicleTokenInterceptorTest {

    private static final String VEHICLE_ID = "vehicle-001";
    private static final String TOKEN = "vehicle-token";
    private static final String CHECK_IN_PATH = "/api/v1/vehicles/{vehicleId}/check-in";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleTokenAuthenticator authenticator;

    @Test
    @DisplayName("VEHICLE-POL-003: 토큰과 경로 차량을 인증 포트에 넘기고 통과하면 요청을 처리한다")
    void passesAuthenticatedRequest() throws Exception {
        mockMvc.perform(post(CHECK_IN_PATH, VEHICLE_ID).header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk());

        verify(authenticator).authenticate(TOKEN, VEHICLE_ID);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 토큰이 없으면 401 UNAUTHORIZED 로 응답한다")
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(post(CHECK_IN_PATH, VEHICLE_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
        verifyNoInteractions(authenticator);
    }

    @ParameterizedTest
    @ValueSource(strings = {TOKEN, "Basic " + TOKEN})
    @DisplayName("VEHICLE-POL-003: Bearer 형식이 아니면 401 UNAUTHORIZED 로 응답한다")
    void rejectsNonBearerToken(String authorization) throws Exception {
        mockMvc.perform(post(CHECK_IN_PATH, VEHICLE_ID).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
        verifyNoInteractions(authenticator);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 폐기했거나 발급하지 않은 토큰이면 401 UNAUTHORIZED 로 응답한다")
    void rejectsInvalidToken() throws Exception {
        willThrow(new BusinessException(AuthErrorCode.UNAUTHORIZED))
                .given(authenticator)
                .authenticate(TOKEN, VEHICLE_ID);

        mockMvc.perform(post(CHECK_IN_PATH, VEHICLE_ID).header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 다른 차량의 토큰이면 403 VEHICLE_MISMATCH 로 응답한다")
    void rejectsTokenOfOtherVehicle() throws Exception {
        willThrow(new BusinessException(VehicleErrorCode.VEHICLE_MISMATCH))
                .given(authenticator)
                .authenticate(TOKEN, VEHICLE_ID);

        mockMvc.perform(post(CHECK_IN_PATH, VEHICLE_ID).header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(VehicleErrorCode.VEHICLE_MISMATCH.getCode()));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 등록 경로는 차량 토큰을 검사하지 않는다")
    void skipsRegisterPath() throws Exception {
        mockMvc.perform(post(EnrollmentKeyWebConfig.REGISTER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-enrollment-key"))
                .andExpect(status().isOk());
        verifyNoInteractions(authenticator);
    }

    @RestController
    static class ProbeController {

        @PostMapping(CHECK_IN_PATH)
        void checkIn() {}

        @PostMapping(EnrollmentKeyWebConfig.REGISTER_PATH)
        void register() {}
    }
}
