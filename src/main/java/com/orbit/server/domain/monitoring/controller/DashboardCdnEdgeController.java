package com.orbit.server.domain.monitoring.controller;

import com.orbit.server.domain.monitoring.dto.response.CdnEdgesResponse;
import com.orbit.server.domain.monitoring.service.CdnEdgeStatusService;
import com.orbit.server.global.common.ApiResponse;
import com.orbit.server.global.config.ApiTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** [Phase2] 대시보드용 CDN Edge 상태 API. 관리자 토큰 인증은 SecurityConfig 가 경로 단위로 적용한다. */
@Tag(name = ApiTags.DASHBOARD_CDN_EDGE)
@RestController
@RequestMapping("/api/v1/dashboard/cdn-edges")
@RequiredArgsConstructor
public class DashboardCdnEdgeController {

    private final CdnEdgeStatusService cdnEdgeStatusService;

    /**
     * 설정의 모든 Edge 의 위치·정상 여부·배정 차량 수·캠페인별 다운로드 성공·실패 수를 조회한다. 대시보드가 지도에 Edge 를 그리고, Edge 장애 때
     * 차량이 다른 Edge 로 옮겨 가는 것을 보여 주는 데 쓴다.
     *
     * <p>정상 여부는 이 요청을 받은 Pod 의 헬스체크 결과라 Pod 마다 잠깐 다를 수 있다.
     *
     * @return 200 과 설정 순서대로의 Edge 상태. 배정 차량이 없으면 0, 다운로드 기록이 없으면 빈 목록
     */
    @GetMapping
    public ApiResponse<CdnEdgesResponse> getEdges() {
        return ApiResponse.success(CdnEdgesResponse.from(cdnEdgeStatusService.getStatuses()));
    }
}
