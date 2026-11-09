package com.orbit.server.domain.edge.service;

import java.util.List;

/**
 * 매니페스트에 내려줄 Edge 선택 결과. 차량은 먼저 {@code primary} 에서 받고, 실패하면 {@code fallbacks} 순서대로 다시 시도한다.
 *
 * @param primary 다운로드 URL 을 만들 Edge
 * @param fallbacks 예비 Edge 목록. 가까운 순서(위치를 모르면 설정 순서)이고, 없으면 빈 목록
 */
public record EdgeSelection(SelectedEdge primary, List<SelectedEdge> fallbacks) {

    public EdgeSelection {
        fallbacks = List.copyOf(fallbacks);
    }
}
