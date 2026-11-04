package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.global.external.cdn.CdnProperties.EdgeProperties;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CdnPropertiesTest {

    private static final String BASE_URL = "orbit.cdn.base-url=";
    private static final String FALLBACK_COUNT = "orbit.cdn.fallback-count=";
    private static final String LOCATION_MAX_AGE = "orbit.cdn.location-max-age=";
    private static final String SEOUL = "orbit.cdn.edges[0].";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CdnConfig.class)
            .withPropertyValues(
                    BASE_URL + "http://cdn.test",
                    FALLBACK_COUNT + "2",
                    LOCATION_MAX_AGE + "10m",
                    "orbit.cdn.default-edge-id=edge-seoul",
                    SEOUL + "id=edge-seoul",
                    SEOUL + "name=서울",
                    SEOUL + "base-url=http://cdn.test:30081",
                    SEOUL + "latitude=37.5665",
                    SEOUL + "longitude=126.9780");

    @Test
    @DisplayName("DOWNLOAD-REQ-001: 설정한 CDN 주소를 읽는다")
    void bindsBaseUrl() {
        contextRunner.run(context ->
                assertThat(context.getBean(CdnProperties.class).baseUrl()).isEqualTo(URI.create("http://cdn.test")));
    }

    @Test
    @DisplayName("DOWNLOAD-REQ-001: CDN 주소가 없으면 앱을 띄우지 않는다")
    void failsWithoutBaseUrl() {
        contextRunner
                .withPropertyValues(BASE_URL)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("기본 Edge id·fallback 개수·위치 유효 시간을 읽는다")
    void bindsEdgeSelectionSettings() {
        contextRunner.run(context -> {
            CdnProperties properties = context.getBean(CdnProperties.class);

            assertThat(properties.defaultEdgeId()).isEqualTo("edge-seoul");
            assertThat(properties.fallbackCount()).isEqualTo(2);
            assertThat(properties.locationMaxAge()).isEqualTo(Duration.ofMinutes(10));
        });
    }

    @Test
    @DisplayName("Edge 목록의 id·이름·주소·좌표를 읽는다")
    void bindsEdges() {
        contextRunner.run(
                context -> assertThat(context.getBean(CdnProperties.class).edges())
                        .containsExactly(new EdgeProperties(
                                "edge-seoul", "서울", URI.create("http://cdn.test:30081"), 37.5665, 126.9780)));
    }

    @Test
    @DisplayName("fallback 개수가 음수면 앱을 띄우지 않는다")
    void failsWithNegativeFallbackCount() {
        contextRunner
                .withPropertyValues(FALLBACK_COUNT + "-1")
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s"})
    @DisplayName("위치 유효 시간이 0 이하면 앱을 띄우지 않는다")
    void failsWithNonPositiveLocationMaxAge(String maxAge) {
        contextRunner
                .withPropertyValues(LOCATION_MAX_AGE + maxAge)
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"latitude", "longitude"})
    @DisplayName("Edge 좌표가 빠지면 앱을 띄우지 않는다")
    void failsWithoutEdgeCoordinate(String coordinate) {
        contextRunner
                .withPropertyValues(SEOUL + coordinate + "=")
                .run(context -> assertThat(context).hasFailed());
    }
}
