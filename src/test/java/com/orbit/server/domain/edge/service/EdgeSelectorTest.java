package com.orbit.server.domain.edge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.orbit.server.domain.edge.entity.Coordinates;
import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.entity.Edges;
import com.orbit.server.domain.edge.exception.EdgeErrorCode;
import com.orbit.server.global.error.BusinessException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EdgeSelectorTest {

    private static final int THRESHOLD = 3;
    private static final int FALLBACK_COUNT = 2;

    private static final Edge SEOUL =
            new Edge("edge-seoul", "서울", URI.create("http://cdn.test:30081"), new Coordinates(37.5665, 126.9780));
    private static final Edge DAEJEON =
            new Edge("edge-daejeon", "대전", URI.create("http://cdn.test:30082"), new Coordinates(36.3504, 127.3845));
    private static final Edge BUSAN =
            new Edge("edge-busan", "부산", URI.create("http://cdn.test:30083"), new Coordinates(35.1796, 129.0756));

    private static final Duration LOCATION_MAX_AGE = Duration.ofMinutes(30);
    // 울산 근처: 부산 → 대전 → 서울 순으로 가깝다
    private static final ObservedLocation NEAR_BUSAN = nearBusan(Duration.ofMinutes(1));

    // 설정 순서는 서울·대전·부산, 기본 Edge 는 대전으로 둬 "기본 Edge 우선" 과 "설정 순서" 를 구분한다
    private final EdgeCatalog catalog = () -> new Edges(List.of(SEOUL, DAEJEON, BUSAN), "edge-daejeon");
    private final EdgeHealthBoard board =
            new EdgeHealthBoard(catalog, new EdgeHealthCheckProperties(Duration.ofSeconds(10), THRESHOLD));
    private final EdgeSelector selector =
            new EdgeSelector(board, catalog, new EdgeSelectionProperties(FALLBACK_COUNT, LOCATION_MAX_AGE));

    @Test
    @DisplayName("MANIFEST-POL-001: 차량 위치에서 가까운 순서로 다운로드 Edge 와 예비 Edge 를 고른다")
    void selectsNearestEdgesInDistanceOrder() {
        EdgeSelection selection = selector.select(NEAR_BUSAN);

        assertThat(selection.primary().edgeId()).isEqualTo("edge-busan");
        assertThat(selection.primary().baseUrl()).isEqualTo(URI.create("http://cdn.test:30083"));
        assertThat(selection.primary().distanceKm()).hasValueCloseTo(50.0, within(5.0));
        assertThat(selection.fallbacks())
                .extracting(SelectedEdge::edgeId)
                .containsExactly("edge-daejeon", "edge-seoul");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 받은 지 유효 시간만큼 지난 위치까지는 거리로 고른다")
    void usesLocationUpToMaxAge() {
        EdgeSelection selection = selector.select(nearBusan(LOCATION_MAX_AGE));

        assertThat(selection.primary().edgeId()).isEqualTo("edge-busan");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 위치가 유효 시간보다 오래됐으면 위치를 모를 때처럼 기본 Edge 를 먼저 둔다")
    void selectsDefaultEdgeWhenLocationIsStale() {
        EdgeSelection selection = selector.select(nearBusan(LOCATION_MAX_AGE.plusMillis(1)));

        assertThat(selection.primary().edgeId()).isEqualTo("edge-daejeon");
        assertThat(selection.primary().distanceKm()).isEmpty();
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 비정상 Edge 는 거리가 가까워도 후보에서 뺀다")
    void excludesUnhealthyEdgeEvenIfNearest() {
        failTimes(BUSAN, THRESHOLD);

        EdgeSelection selection = selector.select(NEAR_BUSAN);

        assertThat(selection.primary().edgeId()).isEqualTo("edge-daejeon");
        assertThat(selection.fallbacks()).extracting(SelectedEdge::edgeId).containsExactly("edge-seoul");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 예비 Edge 는 fallback 개수까지만 넣는다")
    void limitsFallbacksToConfiguredCount() {
        EdgeSelector oneFallback = new EdgeSelector(board, catalog, new EdgeSelectionProperties(1, LOCATION_MAX_AGE));

        EdgeSelection selection = oneFallback.select(NEAR_BUSAN);

        assertThat(selection.fallbacks()).extracting(SelectedEdge::edgeId).containsExactly("edge-daejeon");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 위치를 모르면 기본 Edge 를 첫 번째로 두고 나머지는 설정 순서대로 둔다")
    void selectsDefaultEdgeFirstWithoutLocation() {
        EdgeSelection selection = selector.selectDefault();

        assertThat(selection.primary().edgeId()).isEqualTo("edge-daejeon");
        assertThat(selection.primary().distanceKm()).isEmpty();
        assertThat(selection.fallbacks()).extracting(SelectedEdge::edgeId).containsExactly("edge-seoul", "edge-busan");
    }

    @Test
    @DisplayName("위치를 모르고 기본 Edge 가 비정상이면 정상 Edge 를 설정 순서대로 둔다")
    void skipsUnhealthyDefaultEdge() {
        failTimes(DAEJEON, THRESHOLD);

        EdgeSelection selection = selector.selectDefault();

        assertThat(selection.primary().edgeId()).isEqualTo("edge-seoul");
        assertThat(selection.fallbacks()).extracting(SelectedEdge::edgeId).containsExactly("edge-busan");
    }

    @Test
    @DisplayName("MANIFEST-POL-001: 정상 Edge 가 하나도 없으면 NO_AVAILABLE_EDGE")
    void failsWhenNoHealthyEdge() {
        failTimes(SEOUL, THRESHOLD);
        failTimes(DAEJEON, THRESHOLD);
        failTimes(BUSAN, THRESHOLD);

        assertThatThrownBy(() -> selector.select(NEAR_BUSAN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", EdgeErrorCode.NO_AVAILABLE_EDGE);
        assertThatThrownBy(selector::selectDefault)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", EdgeErrorCode.NO_AVAILABLE_EDGE);
    }

    private static ObservedLocation nearBusan(Duration age) {
        return new ObservedLocation(35.5384, 129.3114, age);
    }

    private void failTimes(Edge edge, int times) {
        for (int i = 0; i < times; i++) {
            board.recordFailure(edge);
        }
    }
}
