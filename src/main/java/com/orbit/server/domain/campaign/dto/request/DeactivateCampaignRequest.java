package com.orbit.server.domain.campaign.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.orbit.server.domain.campaign.entity.CampaignStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

/**
 * 캠페인 비활성화 요청.
 *
 * <p>상태를 바꾸는 요청 모양을 유지하되, 재활성화는 허용하지 않으므로 {@code INACTIVE} 만 받는다.
 *
 * @param status 바꿀 상태. {@code INACTIVE} 만 허용한다
 */
public record DeactivateCampaignRequest(
        @Schema(allowableValues = "INACTIVE") CampaignStatus status) {

    // 직렬화·API 문서에 속성으로 드러나지 않도록 숨긴다
    @JsonIgnore
    @AssertTrue
    public boolean isInactive() {
        return status == CampaignStatus.INACTIVE;
    }
}
