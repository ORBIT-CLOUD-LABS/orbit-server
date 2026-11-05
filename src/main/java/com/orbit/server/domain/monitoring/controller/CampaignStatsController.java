package com.orbit.server.domain.monitoring.controller;

import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.monitoring.dto.response.CampaignStatsResponse;
import com.orbit.server.domain.monitoring.service.CampaignStatsService;
import com.orbit.server.global.common.ApiResponse;
import com.orbit.server.global.config.ApiTags;
import com.orbit.server.global.error.ApiErrorCodes;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 캠페인 진행 현황 API. 관리자 토큰 인증은 SecurityConfig 가 경로 단위로 적용한다.
 *
 * <p>경로는 캠페인 API 아래지만, 여러 컨텍스트의 테이블을 읽는 조회 모델이라 monitoring 에 둔다.
 */
@Tag(name = ApiTags.ADMIN_CAMPAIGN)
@RestController
@RequestMapping("/api/v1/admin/campaigns")
@RequiredArgsConstructor
public class CampaignStatsController {

    private final CampaignStatsService campaignStatsService;

    /**
     * 캠페인의 대상·성공·실패(사유별)·진행 중 차량 수를 조회한다. 비활성이거나 적용 기간이 끝난 캠페인도 조회한다.
     *
     * @param campaignId 캠페인 ID
     * @return 200 과 캠페인 진행 현황
     */
    @GetMapping("/{campaignId}/stats")
    @ApiErrorCodes(
            type = CampaignErrorCode.class,
            codes = {"CAMPAIGN_NOT_FOUND"})
    public ApiResponse<CampaignStatsResponse> getStats(@PathVariable Long campaignId) {
        return ApiResponse.success(CampaignStatsResponse.from(campaignStatsService.getStats(campaignId)));
    }
}
