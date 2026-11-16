package com.orbit.server.domain.monitoring.service;

import java.util.List;

/**
 * Edge·캠페인별 업데이트 성공·실패 수. Edge 하나의 캠페인별 수를 골라 준다.
 *
 * <p>집계에는 결과가 있는 Edge 만 나오므로, 대시보드가 Edge 마다 값이 있는지 확인하지 않도록 집계에 없는 Edge 는 빈 목록으로 답한다.
 */
public class EdgeDownloadCounts {

    private final List<EdgeDownloadCount> counts;

    /**
     * @param counts Edge·캠페인별 성공·실패 수. 없으면 빈 목록
     */
    public EdgeDownloadCounts(List<EdgeDownloadCount> counts) {
        this.counts = List.copyOf(counts);
    }

    /**
     * @param edgeId 찾을 Edge 의 id
     * @return 이 Edge 에서 파일을 받은 결과가 있는 캠페인별 성공·실패 수. 받은 순서를 지키고 수정할 수 없다. 결과가 없으면 빈 목록
     */
    public List<EdgeDownloadCount> of(String edgeId) {
        return counts.stream().filter(count -> count.edgeId().equals(edgeId)).toList();
    }
}
