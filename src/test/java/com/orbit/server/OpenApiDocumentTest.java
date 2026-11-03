package com.orbit.server;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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

/** springdoc 을 켠 상태에서 컨트롤러 Javadoc 과 응답 형식이 API 문서에 반영되는지 확인한다. */
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocumentTest {

    private static final String API_DOCS_URL = "/v3/api-docs";
    private static final String CHECK_IN = "$.paths['/api/v1/vehicles/{vehicleId}/check-in'].post";
    private static final String ARTIFACT_DETAIL = "$.paths['/api/v1/admin/artifacts/{artifactId}'].get";
    private static final String SNAPSHOT = "$.paths['/api/v1/dashboard/vehicles'].get";
    private static final String STREAM = "$.paths['/api/v1/dashboard/vehicles/stream'].get";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("컨트롤러 메서드 Javadoc 을 API 요약과 설명으로 쓴다")
    void usesJavadocAsOperationDescription() throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath(CHECK_IN + ".summary").value("차량의 현재 버전을 기록하고 업데이트할 캠페인이 있는지 응답한다"))
                .andExpect(jsonPath(CHECK_IN + ".description", startsWith("차량의 현재 버전을 기록하고")));
    }

    @Test
    @DisplayName("Javadoc @param 을 파라미터 설명으로 쓴다")
    void usesJavadocAsParameterDescription() throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(
                        jsonPath(ARTIFACT_DETAIL + ".parameters[0].description").value("Artifact ID"));
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "$.paths['/api/v1/admin/artifacts'].post | 관리자 · 업데이트 파일",
                "$.paths['/api/v1/admin/campaigns'].post | 관리자 · 캠페인",
                "$.paths['/api/v1/dashboard/vehicles'].get | 대시보드 · 차량 상태",
                "$.paths['/api/v1/vehicles/register'].post | 차량 · 등록",
                "$.paths['/api/v1/vehicles/{vehicleId}/check-in'].post | 차량 · 업데이트",
                "$.paths['/api/v1/vehicles/{vehicleId}/campaigns/{campaignId}/manifest'].get | 차량 · 업데이트"
            })
    @DisplayName("API 를 클래스 이름이 아닌 업무 단위 Tag 로 묶는다")
    void groupsOperationsByTag(String operation, String tag) throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath(operation + ".tags[0]").value(tag));
    }

    @Test
    @DisplayName("성공 응답 형식은 JSON 으로, SSE 는 text/event-stream 으로 표시한다")
    void showsResponseMediaType() throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath(SNAPSHOT + ".responses['200'].content['application/json']")
                        .exists())
                .andExpect(
                        jsonPath(SNAPSHOT + ".responses['200'].content['*/*']").doesNotExist())
                .andExpect(jsonPath(STREAM + ".responses['200'].content['text/event-stream']")
                        .exists());
    }
}
