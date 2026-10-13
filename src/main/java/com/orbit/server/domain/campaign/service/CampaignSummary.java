package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.campaign.entity.CampaignStatus;
import java.time.Instant;

/**
 * 관리자가 캠페인 목록에서 보는 캠페인 정보.
 *
 * <p>조회 전용이라 Campaign 에 getter 를 두지 않고 DB 에서 바로 이 형태로 읽는다. 목표 버전은 Artifact 에 있으므로 여기에 담지 않고
 * {@code artifactId} 로 따로 읽는다.
 *
 * @param campaignId 캠페인 ID
 * @param artifactId 배포할 Artifact 의 ID. 목표 버전을 읽을 때 쓴다
 * @param name 캠페인 이름
 * @param model 대상 차종
 * @param status 캠페인 상태
 * @param startAt 적용 시작 시각
 * @param endAt 적용 종료 시각
 * @param createdAt 등록 시각
 */
public record CampaignSummary(
        Long campaignId,
        Long artifactId,
        String name,
        String model,
        CampaignStatus status,
        Instant startAt,
        Instant endAt,
        Instant createdAt) {}
