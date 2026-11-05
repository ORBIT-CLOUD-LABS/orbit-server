package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import com.orbit.server.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자가 고른 캠페인의 배포를 중지한다. 비활성 캠페인은 이후 체크인 대상 판정과 매니페스트 발급에서 빠진다.
 *
 * <p>응답이 캠페인 상세와 같은 형식이라 상세 변환은 {@link CampaignDetailService} 에 맡긴다.
 */
@Service
@RequiredArgsConstructor
public class CampaignDeactivationService {

    private final CampaignRepository campaignRepository;
    private final CampaignDetailService campaignDetailService;

    /**
     * 캠페인을 비활성으로 바꾼다. 이미 비활성이면 그대로 둔다.
     *
     * @param campaignId 캠페인 ID
     * @return 비활성화한 캠페인 상세
     * @throws BusinessException 캠페인이 없으면 CAMPAIGN_NOT_FOUND
     */
    // 변경 감지로 저장하고, 같은 영속성 컨텍스트에서 바뀐 상태를 상세로 읽도록 한 트랜잭션으로 묶는다
    @Transactional
    public CampaignDetail deactivate(Long campaignId) {
        Campaign campaign = campaignRepository
                .findById(campaignId)
                .orElseThrow(() -> new BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));
        campaign.deactivate();
        return campaignDetailService.getById(campaignId);
    }
}
