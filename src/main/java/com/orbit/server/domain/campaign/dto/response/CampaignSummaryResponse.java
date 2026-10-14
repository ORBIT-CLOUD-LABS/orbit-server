package com.orbit.server.domain.campaign.dto.response;

import com.orbit.server.domain.campaign.entity.CampaignStatus;
import com.orbit.server.domain.campaign.service.CampaignListItem;
import com.orbit.server.domain.campaign.service.CampaignSummary;
import java.time.Instant;

/**
 * 캠페인 목록의 한 항목. 대상 HW 버전·지역·현재 버전 범위는 상세 조회에서 보여 준다.
 *
 * @param campaignId 캠페인 ID
 * @param name 캠페인 이름
 * @param model 대상 차종
 * @param targetVersion 목표 버전
 * @param status 캠페인 상태
 * @param startAt 적용 시작 시각
 * @param endAt 적용 종료 시각
 * @param createdAt 등록 시각
 */
public record CampaignSummaryResponse(
        Long campaignId,
        String name,
        String model,
        String targetVersion,
        CampaignStatus status,
        Instant startAt,
        Instant endAt,
        Instant createdAt) {

    static CampaignSummaryResponse from(CampaignListItem item) {
        CampaignSummary campaign = item.campaign();
        return new CampaignSummaryResponse(
                campaign.campaignId(),
                campaign.name(),
                campaign.model(),
                item.targetVersion(),
                campaign.status(),
                campaign.startAt(),
                campaign.endAt(),
                campaign.createdAt());
    }
}
