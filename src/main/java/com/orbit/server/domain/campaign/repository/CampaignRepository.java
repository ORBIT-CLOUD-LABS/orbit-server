package com.orbit.server.domain.campaign.repository;

import com.orbit.server.domain.campaign.entity.Campaign;
import java.util.List;
import java.util.Optional;

/** 등록한 캠페인 저장소. */
public interface CampaignRepository {

    /**
     * 이 차종을 대상으로 하는 활성 캠페인을 조회한다. 중복 판정과 체크인 대상 판정에서 후보를 좁힐 때 쓴다.
     *
     * <p>HW 버전·지역·버전 범위는 여러 테이블에 걸치거나 문자열 비교가 틀리므로 DB 에서 거르지 않고, 차종으로만 좁힌 뒤 도메인에서 판정한다.
     *
     * @param model 대상 차종
     * @return 이 차종의 활성 캠페인. 없으면 빈 목록
     */
    List<Campaign> findAllActiveByModel(String model);

    /**
     * 캠페인을 조회한다. 매니페스트 발급에서 요청한 캠페인을 판정할 때 쓴다.
     *
     * @param campaignId 캠페인 ID
     * @return 이 ID 의 캠페인. 없으면 빈 값. 활성 여부는 보지 않는다
     */
    Optional<Campaign> findById(Long campaignId);

    /**
     * @param campaignId 캠페인 ID
     * @return 이 ID 의 캠페인이 있으면 true. 활성 여부는 보지 않는다
     */
    boolean existsById(Long campaignId);

    /**
     * 캠페인을 저장한다.
     *
     * @param campaign 저장할 캠페인
     * @return ID 가 채워진 캠페인
     */
    Campaign save(Campaign campaign);
}
