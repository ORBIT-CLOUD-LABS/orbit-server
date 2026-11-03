package com.orbit.server.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** springdoc 을 켠 상태에서 API 문서가 인증 없이 열리고, 경로별로 실제 인증과 같은 스키마가 표시되는지 확인한다. */
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiSecurityConfigTest {

    private static final String API_DOCS_URL = "/v3/api-docs";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("springdoc 이 켜져 있으면 API 문서를 인증 없이 응답한다")
    void servesApiDocsWithoutToken() throws Exception {
        mockMvc.perform(get(API_DOCS_URL)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                OpenApiSecurityConfig.ADMIN_TOKEN,
                OpenApiSecurityConfig.VEHICLE_TOKEN,
                OpenApiSecurityConfig.ENROLLMENT_KEY
            })
    @DisplayName("세 인증 방식을 Bearer 스키마로 등록한다")
    void registersBearerSchemes(String scheme) throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(
                        jsonPath("$.components.securitySchemes.%s.type", scheme).value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.%s.scheme", scheme)
                        .value("bearer"));
    }

    @ParameterizedTest
    @CsvSource({
        "/api/v1/admin/artifacts, post, adminToken",
        "/api/v1/admin/artifacts/{artifactId}, get, adminToken",
        "/api/v1/admin/campaigns, get, adminToken",
        "/api/v1/dashboard/vehicles, get, adminToken",
        "/api/v1/dashboard/vehicles/stream, get, adminToken",
        "/api/v1/vehicles/register, post, enrollmentKey",
        "/api/v1/vehicles/{vehicleId}/check-in, post, vehicleToken",
        "/api/v1/vehicles/{vehicleId}/campaigns/{campaignId}/manifest, get, vehicleToken"
    })
    @DisplayName("경로마다 실제 인증과 같은 스키마 하나만 표시한다")
    void requiresSchemeByPath(String path, String method, String scheme) throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath("$.paths['%s'].%s.security.length()", path, method)
                        .value(1))
                .andExpect(jsonPath("$.paths['%s'].%s.security[0].%s", path, method, scheme)
                        .exists());
    }
}
