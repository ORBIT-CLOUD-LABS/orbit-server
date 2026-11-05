package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.entity.CampaignCondition;
import com.orbit.server.domain.campaign.entity.CampaignPeriod;
import com.orbit.server.domain.campaign.entity.CampaignStatus;
import java.time.Instant;

/**
 * 관리자가 보는 캠페인 상세. 등록 요청과 같은 모양에 캠페인 ID·목표 버전·상태·등록 시각을 더한다.
 *
 * @param campaignId 캠페인 ID
 * @param name 캠페인 이름
 * @param artifactId 배포할 Artifact ID
 * @param targetVersion 목표 버전. 캠페인이 배포하는 Artifact 의 버전
 * @param target 대상 조건
 * @param status 캠페인 상태
 * @param startAt 적용 시작 시각
 * @param endAt 적용 종료 시각
 * @param createdAt 등록 시각
 */
public record CampaignDetail(
        Long campaignId,
        String name,
        Long artifactId,
        String targetVersion,
        CampaignTargetDetail target,
        CampaignStatus status,
        Instant startAt,
        Instant endAt,
        Instant createdAt) {

    /**
     * 대상 조건을 지연 로딩하므로 트랜잭션 안에서 부른다.
     *
     * @param campaign 캠페인
     * @param targetVersion 캠페인이 배포하는 Artifact 의 버전
     * @return 캠페인 상세
     */
    static CampaignDetail of(Campaign campaign, String targetVersion) {
        CampaignCondition condition = campaign.getCondition();
        CampaignPeriod period = condition.getPeriod();
        return new CampaignDetail(
                campaign.getId(),
                campaign.getName().value(),
                campaign.getArtifactId(),
                targetVersion,
                CampaignTargetDetail.of(condition),
                campaign.getStatus(),
                period.startAt(),
                period.endAt(),
                campaign.getCreatedAt());
    }
}
