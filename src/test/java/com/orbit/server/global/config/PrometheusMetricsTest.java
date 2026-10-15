package com.orbit.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Phase1 실험에서 Prometheus 로 서버 전체의 응답 시간 분위수를 계산할 수 있는 형식으로 메트릭을 내보내는지 확인한다. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PrometheusMetricsTest {

    private static final String PROMETHEUS_URL = "/actuator/prometheus";

    @Autowired
    private MockMvc mockMvc;

    // HTTP 요청 메트릭은 요청이 한 번 이상 처리된 뒤에 생기므로 먼저 요청을 보낸다
    @BeforeEach
    void recordRequest() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // 분위수를 Pod 마다 미리 계산해 내보내면 Pod 끼리 합칠 수 없으므로, 구간별 개수(bucket)로 내보내 histogram_quantile 로 합친다
    @Test
    @DisplayName("HTTP 요청 응답 시간을 구간별 개수(bucket)로 내보낸다")
    void exportsHttpRequestHistogram() throws Exception {
        assertThat(scrape()).contains("http_server_requests_seconds_bucket{");
    }

    @Test
    @DisplayName("다른 앱의 메트릭과 구분할 수 있도록 모든 메트릭에 application 태그를 붙인다")
    void tagsMetricsWithApplicationName() throws Exception {
        assertThat(scrape()).contains("jvm_memory_used_bytes{application=\"orbit-server\"");
    }

    private String scrape() throws Exception {
        return mockMvc.perform(get(PROMETHEUS_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
