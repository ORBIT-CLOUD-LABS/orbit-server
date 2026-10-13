package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.campaign.entity.CampaignStatus;
import org.springframework.util.StringUtils;

/**
 * 캠페인 목록 필터. 값이 없는 조건은 거르지 않는다.
 *
 * @param status 캠페인 상태. null 이면 모든 상태
 * @param model 대상 차종. null 이거나 공백이면 모든 차종
 */
public record CampaignFilter(CampaignStatus status, String model) {

    public CampaignFilter {
        // ?model= 처럼 값 없이 온 필터는 필터가 없는 것으로 본다
        if (!StringUtils.hasText(model)) {
            model = null;
        }
    }
}
