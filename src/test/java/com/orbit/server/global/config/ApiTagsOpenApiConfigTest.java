package com.orbit.server.global.config;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.orbit.server.TestcontainersConfiguration;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
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
    @DisplayName("모든 API 는 정의한 Tag 중 하나로 묶인다. Tag 를 빼먹으면 springdoc 이 클래스 이름 Tag 를 붙여 실패한다")
    void tagsEveryOperationWithDefinedTag() throws Exception {
        List<String> definedNames = ApiTags.ordered().stream().map(Tag::getName).toList();
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath("$.paths.*.*.tags[*]").value(everyItem(in(definedNames))));
    }

    @Test
    @DisplayName("컨트롤러에 설명을 적지 않아도 Tag 설명은 정의한 값으로 표시한다")
    void showsDefinedDescription() throws Exception {
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(jsonPath("$.tags[?(@.name == '%s')].description", ApiTags.ADMIN_ARTIFACT)
                        .value(contains("Origin 에 올린 업데이트 파일(Artifact) 등록·조회")));
    }
}
