package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.artifact.service.ArtifactQueryService;
import com.orbit.server.domain.artifact.service.ArtifactSummary;
import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.entity.TargetRelease;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.InvalidValueException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자가 고른 파일로 캠페인을 등록한다. 등록 즉시 활성 상태가 되어 체크인 대상 판정에 반영된다. (CAMPAIGN-REQ-001 /
 * CAMPAIGN-POL-001)
 *
 * <p>다른 컨텍스트인 Artifact 는 {@link ArtifactQueryService} 로만 읽는다.
 */
@Service
@RequiredArgsConstructor
public class CampaignRegistrationService {

    private final ArtifactQueryService artifactQueryService;
    private final CampaignRepository campaignRepository;

    /**
     * 파일 잠금·확인 → 차종·목표 버전 검증 → 중복 활성 캠페인 확인 → 저장 순서로 등록한다.
     *
     * <p>중복 확인은 HW 버전·지역이 여러 테이블에 걸쳐 DB 제약으로 막을 수 없어 앱에서 한다. 같은 요청이 동시에 이 확인을 통과하지
     * 않도록, 고른 파일 행을 커밋할 때까지 잠가 같은 파일의 등록 요청을 순서대로 처리한다.
     *
     * @param command 등록 요청
     * @return 등록한 캠페인 ID
     * @throws BusinessException 파일이 없으면 ARTIFACT_NOT_FOUND, 중복 활성 캠페인이 있으면 ACTIVE_CAMPAIGN_EXISTS
     * @throws InvalidValueException 대상 차종이나 목표 버전이 파일과 다른 경우
     */
    // 파일 잠금을 저장·커밋까지 유지하고 HW 버전·지역 컬렉션을 지연 로딩해 비교하므로 조회부터 저장까지 한 트랜잭션으로 묶는다
    @Transactional
    public Long register(RegisterCampaignCommand command) {
        // 잠금이 첫 조회여야 앞선 요청이 커밋한 캠페인을 중복 확인에서 볼 수 있다
        ArtifactSummary artifact = artifactQueryService.lockAndGetById(command.artifactId());
        TargetRelease release = command.release();
        release.requireSameAs(new TargetRelease(artifact.model(), artifact.version()));
        Campaign campaign = command.toCampaign();
        requireNoDuplicate(campaign, release.model());
        return campaignRepository.save(campaign).id();
    }

    private void requireNoDuplicate(Campaign campaign, String model) {
        boolean duplicated =
                campaignRepository.findAllActiveByModel(model).stream().anyMatch(campaign::duplicates);
        if (duplicated) {
            throw new BusinessException(CampaignErrorCode.ACTIVE_CAMPAIGN_EXISTS);
        }
    }
}
