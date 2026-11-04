package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.edge.entity.Coordinates;
import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.entity.Edges;
import com.orbit.server.domain.edge.service.EdgeCatalog;
import com.orbit.server.global.error.InvalidValueException;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ConfiguredEdgeCatalogTest {

    private static final String SEOUL = "orbit.cdn.edges[0].";
    private static final String BUSAN = "orbit.cdn.edges[1].";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CdnConfig.class, ConfiguredEdgeCatalog.class)
            .withPropertyValues(
                    "orbit.cdn.base-url=http://cdn.test",
                    "orbit.cdn.fallback-count=2",
                    "orbit.cdn.location-max-age=10m",
                    "orbit.cdn.default-edge-id=edge-seoul",
                    SEOUL + "id=edge-seoul",
                    SEOUL + "name=서울",
                    SEOUL + "base-url=http://cdn.test:30081",
                    SEOUL + "latitude=37.5665",
                    SEOUL + "longitude=126.9780",
                    BUSAN + "id=edge-busan",
                    BUSAN + "name=부산",
                    BUSAN + "base-url=http://cdn.test:30083",
                    BUSAN + "latitude=35.1796",
                    BUSAN + "longitude=129.0756");

    @Test
    @DisplayName("설정한 Edge 목록과 기본 Edge 를 조회한다")
    void loadsConfiguredEdges() {
        Edge seoul =
                new Edge("edge-seoul", "서울", URI.create("http://cdn.test:30081"), new Coordinates(37.5665, 126.9780));
        Edge busan =
                new Edge("edge-busan", "부산", URI.create("http://cdn.test:30083"), new Coordinates(35.1796, 129.0756));

        contextRunner.run(context -> {
            Edges edges = context.getBean(EdgeCatalog.class).edges();

            assertThat(edges.all()).containsExactly(seoul, busan);
            assertThat(edges.defaultEdge()).isEqualTo(seoul);
        });
    }

    @Test
    @DisplayName("Edge id 가 중복되면 앱을 띄우지 않는다")
    void failsWithDuplicateId() {
        contextRunner
                .withPropertyValues(BUSAN + "id=edge-seoul")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasRootCauseInstanceOf(InvalidValueException.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"latitude=90.1", "latitude=-90.1", "longitude=180.1", "longitude=-180.1"})
    @DisplayName("Edge 좌표가 범위를 벗어나면 앱을 띄우지 않는다")
    void failsWithCoordinatesOutOfRange(String coordinate) {
        contextRunner
                .withPropertyValues(SEOUL + coordinate)
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasRootCauseInstanceOf(InvalidValueException.class));
    }

    @Test
    @DisplayName("default-edge-id 가 목록에 없으면 앱을 띄우지 않는다")
    void failsWithUnknownDefaultEdgeId() {
        contextRunner
                .withPropertyValues("orbit.cdn.default-edge-id=edge-daejeon")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasRootCauseInstanceOf(InvalidValueException.class));
    }

    @Test
    @DisplayName("Edge 목록이 비어 있으면 앱을 띄우지 않는다")
    void failsWithoutEdges() {
        new ApplicationContextRunner()
                .withUserConfiguration(CdnConfig.class, ConfiguredEdgeCatalog.class)
                .withPropertyValues(
                        "orbit.cdn.base-url=http://cdn.test",
                        "orbit.cdn.fallback-count=2",
                        "orbit.cdn.location-max-age=10m",
                        "orbit.cdn.default-edge-id=edge-seoul")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasRootCauseInstanceOf(InvalidValueException.class));
    }
}
