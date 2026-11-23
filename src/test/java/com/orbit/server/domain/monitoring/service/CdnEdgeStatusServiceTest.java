package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.orbit.server.domain.edge.service.EdgeHealthBoard;
import com.orbit.server.domain.edge.service.result.EdgeStatus;
import com.orbit.server.domain.monitoring.repository.EdgeUsageRepository;
import com.orbit.server.domain.monitoring.service.count.EdgeDownloadCount;
import com.orbit.server.domain.monitoring.service.count.EdgeVehicleCount;
import com.orbit.server.domain.monitoring.service.result.CdnEdgeStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CdnEdgeStatusServiceTest {

    private static final EdgeStatus SEOUL = new EdgeStatus("edge-seoul", "서울", 37.5665, 126.9780, true);
    private static final EdgeStatus BUSAN = new EdgeStatus("edge-busan", "부산", 35.1796, 129.0756, false);

    private final EdgeHealthBoard edgeHealthBoard = mock(EdgeHealthBoard.class);
    private final EdgeUsageRepository edgeUsageRepository = mock(EdgeUsageRepository.class);
    private final CdnEdgeStatusService service = new CdnEdgeStatusService(edgeHealthBoard, edgeUsageRepository);

    @Test
    @DisplayName("CDN Edge 상태: 설정 순서대로 Edge 의 위치·정상 여부에 배정 차량 수와 캠페인별 다운로드 수를 합친다")
    void combinesEdgesWithUsage() {
        EdgeDownloadCount seoulFirst = new EdgeDownloadCount("edge-seoul", 7L, 40, 2);
        EdgeDownloadCount seoulSecond = new EdgeDownloadCount("edge-seoul", 9L, 3, 0);
        EdgeDownloadCount busan = new EdgeDownloadCount("edge-busan", 7L, 0, 1);
        given(edgeHealthBoard.statuses()).willReturn(List.of(SEOUL, BUSAN));
        given(edgeUsageRepository.findAssignedVehicleCounts())
                .willReturn(List.of(new EdgeVehicleCount("edge-busan", 7), new EdgeVehicleCount("edge-seoul", 42)));
        given(edgeUsageRepository.findDownloadCounts()).willReturn(List.of(busan, seoulFirst, seoulSecond));

        assertThat(service.getStatuses())
                .containsExactly(
                        new CdnEdgeStatus(
                                "edge-seoul", "서울", 37.5665, 126.9780, true, 42, List.of(seoulFirst, seoulSecond)),
                        new CdnEdgeStatus("edge-busan", "부산", 35.1796, 129.0756, false, 7, List.of(busan)));
    }

    @Test
    @DisplayName("CDN Edge 상태: 배정 차량도 다운로드 기록도 없는 Edge 는 0 과 빈 목록으로 응답한다")
    void fillsEdgeWithoutUsage() {
        given(edgeHealthBoard.statuses()).willReturn(List.of(SEOUL, BUSAN));
        given(edgeUsageRepository.findAssignedVehicleCounts()).willReturn(List.of());
        given(edgeUsageRepository.findDownloadCounts()).willReturn(List.of());

        assertThat(service.getStatuses())
                .containsExactly(
                        new CdnEdgeStatus("edge-seoul", "서울", 37.5665, 126.9780, true, 0, List.of()),
                        new CdnEdgeStatus("edge-busan", "부산", 35.1796, 129.0756, false, 0, List.of()));
    }

    @Test
    @DisplayName("CDN Edge 상태: 설정에서 빠진 Edge 의 과거 기록은 응답하지 않는다")
    void skipsUsageOfRemovedEdge() {
        given(edgeHealthBoard.statuses()).willReturn(List.of(SEOUL));
        given(edgeUsageRepository.findAssignedVehicleCounts())
                .willReturn(List.of(new EdgeVehicleCount("edge-removed", 5)));
        given(edgeUsageRepository.findDownloadCounts())
                .willReturn(List.of(new EdgeDownloadCount("edge-removed", 7L, 1, 1)));

        assertThat(service.getStatuses())
                .containsExactly(new CdnEdgeStatus("edge-seoul", "서울", 37.5665, 126.9780, true, 0, List.of()));
    }
}
