package com.orbit.server.domain.edge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.edge.entity.Coordinates;
import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.entity.Edges;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class EdgeHealthBoardTest {

    private static final int THRESHOLD = 3;

    private static final Edge SEOUL =
            new Edge("edge-seoul", "서울", URI.create("http://cdn.test:30081"), new Coordinates(37.5665, 126.9780));
    private static final Edge DAEJEON =
            new Edge("edge-daejeon", "대전", URI.create("http://cdn.test:30082"), new Coordinates(36.3504, 127.3845));
    private static final Edge BUSAN =
            new Edge("edge-busan", "부산", URI.create("http://cdn.test:30083"), new Coordinates(35.1796, 129.0756));

    private final EdgeHealthBoard board = new EdgeHealthBoard(
            () -> new Edges(List.of(SEOUL, DAEJEON, BUSAN), "edge-seoul"),
            new EdgeHealthCheckProperties(Duration.ofSeconds(10), THRESHOLD));

    @Test
    @DisplayName("첫 확인 전에는 모든 Edge 를 설정 순서대로 정상 Edge 로 돌려준다")
    void startsWithAllEdgesHealthy() {
        assertThat(board.healthyEdges()).containsExactly(SEOUL, DAEJEON, BUSAN);
    }

    @Test
    @DisplayName("연속 실패가 기준 횟수에 이른 Edge 는 정상 Edge 에서 뺀다")
    void excludesEdgeAfterConsecutiveFailures() {
        failTimes(DAEJEON, THRESHOLD);

        assertThat(board.healthyEdges()).containsExactly(SEOUL, BUSAN);
        assertThat(board.isHealthy(DAEJEON)).isFalse();
    }

    @Test
    @DisplayName("연속 실패가 기준 횟수보다 적으면 정상 Edge 에 남긴다")
    void keepsEdgeBelowThreshold() {
        failTimes(DAEJEON, THRESHOLD - 1);

        assertThat(board.healthyEdges()).containsExactly(SEOUL, DAEJEON, BUSAN);
    }

    @Test
    @DisplayName("빠진 Edge 가 한 번 성공하면 원래 순서 자리로 되돌린다")
    void restoresEdgeAfterOneSuccess() {
        failTimes(DAEJEON, THRESHOLD);

        board.recordSuccess(DAEJEON);

        assertThat(board.healthyEdges()).containsExactly(SEOUL, DAEJEON, BUSAN);
    }

    @Test
    @DisplayName("모든 Edge 가 비정상이면 빈 목록을 돌려준다")
    void returnsEmptyWhenAllUnhealthy() {
        failTimes(SEOUL, THRESHOLD);
        failTimes(DAEJEON, THRESHOLD);
        failTimes(BUSAN, THRESHOLD);

        assertThat(board.healthyEdges()).isEmpty();
    }

    @Test
    @DisplayName("비정상으로 바뀔 때와 복구될 때 한 번씩만 로그를 남긴다")
    void logsOnlyStateChanges(CapturedOutput output) {
        failTimes(DAEJEON, THRESHOLD + 2);
        board.recordSuccess(DAEJEON);
        board.recordSuccess(DAEJEON);

        assertThat(countOf(output, "Edge 가 비정상으로 바뀌었습니다: edgeId=edge-daejeon")).isEqualTo(1);
        assertThat(countOf(output, "Edge 가 복구되었습니다: edgeId=edge-daejeon")).isEqualTo(1);
    }

    private void failTimes(Edge edge, int times) {
        for (int i = 0; i < times; i++) {
            board.recordFailure(edge);
        }
    }

    private static int countOf(CapturedOutput output, String message) {
        return output.getOut().split(message, -1).length - 1;
    }
}
