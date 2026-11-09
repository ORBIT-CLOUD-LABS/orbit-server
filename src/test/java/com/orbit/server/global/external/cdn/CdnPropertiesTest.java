package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.global.external.cdn.CdnProperties.EdgeProperties;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CdnPropertiesTest {

    private static final String SEOUL = "orbit.cdn.edges[0].";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CdnConfig.class)
            .withPropertyValues(
                    "orbit.cdn.default-edge-id=edge-seoul",
                    SEOUL + "id=edge-seoul",
                    SEOUL + "name=서울",
                    SEOUL + "base-url=http://cdn.test:30081",
                    SEOUL + "latitude=37.5665",
                    SEOUL + "longitude=126.9780");

    @Test
    @DisplayName("기본 Edge id 를 읽는다")
    void bindsDefaultEdgeId() {
        contextRunner.run(context ->
                assertThat(context.getBean(CdnProperties.class).defaultEdgeId()).isEqualTo("edge-seoul"));
    }

    @Test
    @DisplayName("Edge 목록의 id·이름·주소·좌표를 읽는다")
    void bindsEdges() {
        contextRunner.run(
                context -> assertThat(context.getBean(CdnProperties.class).edges())
                        .containsExactly(new EdgeProperties(
                                "edge-seoul", "서울", URI.create("http://cdn.test:30081"), 37.5665, 126.9780)));
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
