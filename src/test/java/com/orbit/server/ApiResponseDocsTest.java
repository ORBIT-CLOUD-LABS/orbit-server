package com.orbit.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** springdoc 을 켠 상태에서 API 별 성공 상태와 오류 코드가 실제 컨트롤러·서비스 동작대로 문서에 표시되는지 확인한다. */
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiResponseDocsTest {

    private static final String API_DOCS_URL = "/v3/api-docs";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({
        "/api/v1/admin/artifacts, post, ArtifactRegistrationResponse",
        "/api/v1/admin/campaigns, post, CampaignRegistrationResponse"
    })
    @DisplayName("등록 API 는 성공 응답을 200 이 아닌 201 로 표시한다")
    void showsCreatedForRegistration(String path, String method, String schema) throws Exception {
        String responses = "$.paths['%s'].%s.responses".formatted(path, method);
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath(responses + "['201'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/" + schema))
                .andExpect(jsonPath(responses + "['200']").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "/api/v1/admin/artifacts | post | 409 | ARTIFACT_VERSION_EXISTS",
                "/api/v1/admin/artifacts | post | 413 | CONTENT_TOO_LARGE",
                "/api/v1/admin/artifacts | post | 422 | HASH_MISMATCH",
                "/api/v1/admin/artifacts | post | 502 | ORIGIN_UPLOAD_FAILED",
                "/api/v1/admin/artifacts/{artifactId} | get | 404 | ARTIFACT_NOT_FOUND",
                "/api/v1/admin/campaigns | post | 404 | ARTIFACT_NOT_FOUND",
                "/api/v1/admin/campaigns | post | 409 | ACTIVE_CAMPAIGN_EXISTS"
            })
    @DisplayName("API 별 오류 코드를 HTTP 상태별 응답으로 표시한다")
    void showsApiErrorCodes(String path, String method, String status, String codes) throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath("$.paths['%s'].%s.responses['%s'].description", path, method, status)
                        .value(codes));
    }
}
