package com.orbit.server.domain.campaign.dto.response;

import com.orbit.server.domain.campaign.entity.CampaignStatus;
import com.orbit.server.domain.campaign.service.CampaignDetail;
import java.time.Instant;

/**
 * 캠페인 상세 응답. 등록 요청과 같은 모양에 캠페인 ID·목표 버전·상태·등록 시각을 더한다.
 *
 * @param campaignId 캠페인 ID
 * @param name 캠페인 이름
 * @param artifactId 배포할 Artifact ID
 * @param targetVersion 목표 버전
 * @param target 대상 조건
 * @param status 캠페인 상태
 * @param startAt 적용 시작 시각
 * @param endAt 적용 종료 시각
 * @param createdAt 등록 시각
 */
public record CampaignDetailResponse(
        Long campaignId,
        String name,
        Long artifactId,
        String targetVersion,
        CampaignTargetResponse target,
        CampaignStatus status,
        Instant startAt,
        Instant endAt,
        Instant createdAt) {

    public static CampaignDetailResponse from(CampaignDetail detail) {
        return new CampaignDetailResponse(
                detail.campaignId(),
                detail.name(),
                detail.artifactId(),
                detail.targetVersion(),
                CampaignTargetResponse.from(detail.target()),
                detail.status(),
                detail.startAt(),
                detail.endAt(),
                detail.createdAt());
    }
}
