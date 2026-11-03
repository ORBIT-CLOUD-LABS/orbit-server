package com.orbit.server.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.orbit.server.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** springdoc 을 켠 상태에서 API 문서에 공통 오류 응답이 실제 응답 형식대로 표시되는지 확인한다. */
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ErrorResponseOpenApiConfigTest {

    private static final String API_DOCS_URL = "/v3/api-docs";
    private static final String SNAPSHOT_PATH = "/api/v1/dashboard/vehicles";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("오류 응답 스키마를 code, message 문자열로 등록한다")
    void registersErrorResponseSchema() throws Exception {
        String schema = "$.components.schemas." + ErrorApiResponses.SCHEMA_NAME;
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath(schema + ".properties.code.type").value("string"))
                .andExpect(jsonPath(schema + ".properties.message.type").value("string"));
    }

    @ParameterizedTest
    @CsvSource({
        "/api/v1/admin/artifacts, post",
        "/api/v1/admin/campaigns, get",
        "/api/v1/vehicles/register, post",
        "/api/v1/vehicles/{vehicleId}/check-in, post",
        "/api/v1/dashboard/vehicles, get"
    })
    @DisplayName("모든 API 에 500 INTERNAL_SERVER_ERROR 를 실제 응답 본문 예시와 함께 표시한다")
    void showsInternalServerErrorOnEveryApi(String path, String method) throws Exception {
        String example =
                "$.paths['%s'].%s.responses['500'].content['application/json'].examples.INTERNAL_SERVER_ERROR.value"
                        .formatted(path, method);
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath(example + ".code").value(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath(example + ".message").value(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    @ParameterizedTest
    @CsvSource({
        "/api/v1/admin/artifacts, post",
        "/api/v1/admin/campaigns, get",
        "/api/v1/vehicles/{vehicleId}/check-in, post",
        "/api/v1/vehicles/{vehicleId}/campaigns/{campaignId}/manifest, get"
    })
    @DisplayName("요청 값을 받는 API 에 400 INVALID_INPUT 을 표시한다")
    void showsInvalidInputOnApiWithInput(String path, String method) throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath("$.paths['%s'].%s.responses['400'].description", path, method)
                        .value(CommonErrorCode.INVALID_INPUT.getCode()));
    }

    @Test
    @DisplayName("요청 값을 받지 않는 API 에는 400 을 표시하지 않는다")
    void omitsInvalidInputOnApiWithoutInput() throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath("$.paths['%s'].get.responses['400']", SNAPSHOT_PATH)
                        .doesNotExist());
    }
}
