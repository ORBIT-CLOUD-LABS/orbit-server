package com.orbit.server.global.config;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.orbit.server.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** springdoc 을 켠 상태에서 API 문서의 Tag 목록이 {@link ApiTags} 정의의 순서·설명대로 표시되는지 확인한다. */
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiTagsOpenApiConfigTest {

    private static final String API_DOCS_URL = "/v3/api-docs";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Tag 목록을 정의한 순서대로 표시한다")
    void listsTagsInDefinedOrder() throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath("$.tags[*].name")
                        .value(contains(
                                ApiTags.ADMIN_ARTIFACT,
                                ApiTags.ADMIN_CAMPAIGN,
                                ApiTags.DASHBOARD_VEHICLE,
                                ApiTags.VEHICLE_REGISTRATION,
                                ApiTags.VEHICLE_UPDATE)));
    }

    @Test
    @DisplayName("Tag 설명은 정의한 값으로 표시한다")
    void showsDefinedDescription() throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath("$.tags[?(@.name == '%s')].description", ApiTags.VEHICLE_UPDATE)
                        .value(contains("체크인으로 업데이트 대상을 확인하고 매니페스트로 다운로드 정보를 받는다")));
    }
}
