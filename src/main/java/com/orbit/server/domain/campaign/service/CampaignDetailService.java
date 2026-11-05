package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.artifact.service.ArtifactQueryService;
import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import com.orbit.server.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자가 보는 캠페인 상세를 조회한다.
 *
 * <p>목표 버전은 Artifact 에 있으므로 다른 컨텍스트인 Artifact 를 {@link ArtifactQueryService} 로 읽어 붙인다.
 */
@Service
@RequiredArgsConstructor
public class CampaignDetailService {

    private final CampaignRepository campaignRepository;
    private final ArtifactQueryService artifactQueryService;

    /**
     * @param campaignId 캠페인 ID
     * @return 캠페인 상세와 목표 버전. 활성 여부와 무관하게 조회한다
     * @throws BusinessException 캠페인이 없으면 CAMPAIGN_NOT_FOUND
     */
    // open-in-view 가 꺼져 있어 HW 버전·지역을 지연 로딩하려면 트랜잭션 안에서 변환해야 한다
    @Transactional(readOnly = true)
    public CampaignDetail getById(Long campaignId) {
        Campaign campaign = campaignRepository
                .findById(campaignId)
                .orElseThrow(() -> new BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));
        // campaign.artifact_id 는 외래 키라 Artifact 가 빠지는 일은 없다
        String targetVersion =
                artifactQueryService.getById(campaign.getArtifactId()).version();
        return CampaignDetail.of(campaign, targetVersion);
    }
}
