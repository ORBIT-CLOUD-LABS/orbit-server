package com.orbit.server.domain.campaign.dto.response;

import com.orbit.server.domain.campaign.service.CampaignListItem;
import java.util.List;

/**
 * 캠페인 목록 응답.
 *
 * <p>배열을 바로 내려주지 않고 감싼다. 페이지 정보 같은 필드가 필요해져도 응답 계약을 깨지 않고 추가하기 위해서다.
 *
 * @param campaigns 최근 등록한 순서의 캠페인
 */
public record CampaignListResponse(List<CampaignSummaryResponse> campaigns) {

    public static CampaignListResponse from(List<CampaignListItem> items) {
        return new CampaignListResponse(
                items.stream().map(CampaignSummaryResponse::from).toList());
    }
}
