package com.orbit.server.domain.campaign.service;

/**
 * 캠페인 목록의 한 항목. 캠페인 정보에 Artifact 에서 읽은 목표 버전을 붙인다.
 *
 * @param campaign 캠페인 정보
 * @param targetVersion 목표 버전. 캠페인이 배포하는 Artifact 의 버전
 */
public record CampaignListItem(CampaignSummary campaign, String targetVersion) {}
