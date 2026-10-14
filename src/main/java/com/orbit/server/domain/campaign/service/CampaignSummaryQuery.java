package com.orbit.server.domain.campaign.service;

import java.util.List;

/**
 * 등록한 캠페인 목록 조회.
 *
 * <p>DB 에서 응답 형태로 바로 읽는 조회 전용 경로라 쓰기용 {@link com.orbit.server.domain.campaign.repository.CampaignRepository}
 * 와 나눈다. 구현은 repository 에 둔다.
 *
 * <p>캠페인은 관리자가 직접 등록하는 만큼만 쌓이므로 페이지 없이 전체를 돌려준다.
 */
public interface CampaignSummaryQuery {

    /**
     * @param filter 상태·차종 필터
     * @return 필터에 맞는 캠페인. 최근 등록한 순서. 없으면 빈 목록
     */
    List<CampaignSummary> findAll(CampaignFilter filter);
}
