package com.orbit.server.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        controllers = EnrollmentKeyInterceptorTest.ProbeController.class,
        properties = "orbit.enrollment.key=" + EnrollmentKeyInterceptorTest.ENROLLMENT_KEY)
@Import({EnrollmentKeyInterceptorTest.ProbeController.class, GlobalExceptionHandler.class, SecurityConfig.class})
class EnrollmentKeyInterceptorTest {

    static final String ENROLLMENT_KEY = "test-enrollment-key";
    private static final String OTHER_PATH = "/api/v1/other";

    @Autowired
    private MockMvc mockMvc;

    // @WebMvcTest 가 차량 토큰 인터셉터 설정도 올리므로 포트 구현 대신 넣는다
    @MockitoBean
    private VehicleTokenAuthenticator vehicleTokenAuthenticator;

    @Test
    @DisplayName("VEHICLE-POL-003: 등록 키가 맞으면 등록 요청을 통과시킨다")
    void passesWithValidKey() throws Exception {
        mockMvc.perform(post(EnrollmentKeyWebConfig.REGISTER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ENROLLMENT_KEY))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-key", "Bearer ", ENROLLMENT_KEY, "Basic " + ENROLLMENT_KEY})
    @DisplayName("VEHICLE-POL-003: 등록 키가 다르거나 Bearer 형식이 아니면 401 INVALID_ENROLLMENT_KEY 로 응답한다")
    void rejectsInvalidKey(String authorization) throws Exception {
        mockMvc.perform(post(EnrollmentKeyWebConfig.REGISTER_PATH).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ENROLLMENT_KEY.getCode()));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 등록 키가 없으면 401 INVALID_ENROLLMENT_KEY 로 응답한다")
    void rejectsMissingKey() throws Exception {
        mockMvc.perform(post(EnrollmentKeyWebConfig.REGISTER_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ENROLLMENT_KEY.getCode()));
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 등록 경로가 아니면 등록 키를 검사하지 않는다")
    void skipsOtherPaths() throws Exception {
        mockMvc.perform(post(OTHER_PATH)).andExpect(status().isOk());
    }

    @RestController
    static class ProbeController {

        @PostMapping(EnrollmentKeyWebConfig.REGISTER_PATH)
        void register() {}

        @PostMapping(OTHER_PATH)
        void other() {}
    }
}
