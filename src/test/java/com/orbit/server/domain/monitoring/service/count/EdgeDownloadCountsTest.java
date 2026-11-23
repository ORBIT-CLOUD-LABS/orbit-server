package com.orbit.server.domain.monitoring.service.count;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EdgeDownloadCountsTest {

    private final EdgeDownloadCounts counts = new EdgeDownloadCounts(List.of(
            new EdgeDownloadCount("edge-busan", 7L, 0, 1),
            new EdgeDownloadCount("edge-seoul", 7L, 40, 2),
            new EdgeDownloadCount("edge-seoul", 9L, 3, 0)));

    @Test
    @DisplayName("Edge 의 캠페인별 성공·실패 수만 받은 순서대로 골라 준다")
    void findsCountsOfEdgeInOrder() {
        assertThat(counts.of("edge-seoul"))
                .containsExactly(
                        new EdgeDownloadCount("edge-seoul", 7L, 40, 2), new EdgeDownloadCount("edge-seoul", 9L, 3, 0));
    }

    @Test
    @DisplayName("집계에 없는 Edge 는 빈 목록으로 답한다")
    void answersEmptyForEdgeWithoutDownloads() {
        assertThat(counts.of("edge-daejeon")).isEmpty();
    }
}
