package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.artifact.service.ArtifactQueryService;
import com.orbit.server.domain.artifact.service.ArtifactSummary;
import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.entity.DottedVersion;
import com.orbit.server.domain.campaign.entity.TargetCampaign;
import com.orbit.server.domain.campaign.entity.TargetVehicle;
import com.orbit.server.domain.campaign.exception.CampaignErrorCode;
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import com.orbit.server.global.error.BusinessException;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차량이 업데이트할 캠페인을 고른다. 다른 컨텍스트(체크인, 매니페스트 발급)는 캠페인을 직접 읽지 않고 이 서비스로만 대상 여부를 묻는다.
 * (VEHICLE-POL-001, MANIFEST-POL-001)
 *
 * <p>다른 컨텍스트인 Artifact 는 {@link ArtifactQueryService} 로만 읽는다.
 */
@Service
@RequiredArgsConstructor
public class CampaignQueryService {

    private final CampaignRepository campaignRepository;
    private final ArtifactQueryService artifactQueryService;

    /**
     * 차종으로 좁힌 활성 캠페인 중 조건을 모두 만족하고 아직 설치하지 않은 버전을 배포하는 캠페인을 고른다. 여러 개면 목표 버전이 가장
     * 높은 것, 같으면 나중에 등록한 것을 고른다.
     *
     * <p>목표 버전을 숫자 자리로 읽을 수 없는 캠페인은 순서를 정할 수 없으므로 고르지 않는다.
     *
     * @param vehicle 판정할 차량
     * @param now 판정 시각
     * @return 대상 캠페인 ID. 대상이 아니면 빈 값
     */
    // HW 버전·지역을 지연 로딩하므로 조회부터 판정까지 한 트랜잭션으로 묶는다
    @Transactional(readOnly = true)
    public Optional<Long> findTargetCampaignId(TargetVehicle vehicle, Instant now) {
        return campaignRepository.findAllActiveByModel(vehicle.model()).stream()
                .filter(campaign -> campaign.targets(vehicle, now))
                .flatMap(campaign -> toTargetCampaign(campaign).stream())
                .filter(target -> !target.isInstalledOn(vehicle.currentVersion()))
                .max(TargetCampaign.PRIORITY)
                .map(TargetCampaign::campaignId);
    }

    /**
     * 캠페인이 있는지 확인한다. 차량이 보고한 업데이트 결과를 기록하기 전에 쓴다. (UPDATE-REQ-001)
     *
     * <p>배포를 중지한 비활성 캠페인도 이미 진행 중이던 차량의 결과는 남겨야 하므로 활성 여부는 보지 않는다.
     *
     * @param campaignId 캠페인 ID
     * @return 캠페인이 있으면 true
     */
    @Transactional(readOnly = true)
    public boolean exists(Long campaignId) {
        return campaignRepository.existsById(campaignId);
    }

    /**
     * 요청한 캠페인이 지금 이 차량의 대상인지 다시 확인하고 배포할 파일을 돌려준다. 체크인과 같은 규칙으로 판정하되, 더 높은 버전의
     * 캠페인이 있어도 요청한 캠페인만 본다. (MANIFEST-REQ-001 / MANIFEST-POL-001)
     *
     * <p>판정 순서는 캠페인 없음 → 비활성·기간 밖 → 대상 아님이다.
     *
     * @param campaignId 차량이 요청한 캠페인 ID
     * @param vehicle 판정할 차량. 현재 버전은 마지막 체크인 때 기록한 값
     * @param now 판정 시각
     * @return 캠페인이 배포하는 파일
     * @throws BusinessException 캠페인이 없으면 CAMPAIGN_NOT_FOUND, 비활성이거나 적용 기간 밖이면 CAMPAIGN_INACTIVE, 대상 조건이
     *     맞지 않거나 이미 목표 버전이면 NOT_UPDATE_TARGET
     */
    // HW 버전·지역을 지연 로딩하므로 조회부터 판정까지 한 트랜잭션으로 묶는다
    @Transactional(readOnly = true)
    public ArtifactSummary getTargetArtifact(Long campaignId, TargetVehicle vehicle, Instant now) {
        Campaign campaign = getActiveCampaign(campaignId, now);
        if (!campaign.targets(vehicle, now)) {
            throw new BusinessException(CampaignErrorCode.NOT_UPDATE_TARGET);
        }
        ArtifactSummary artifact = artifactQueryService.getById(campaign.artifactId());
        if (!isDeliverable(campaign, artifact, vehicle)) {
            throw new BusinessException(CampaignErrorCode.NOT_UPDATE_TARGET);
        }
        return artifact;
    }

    private Campaign getActiveCampaign(Long campaignId, Instant now) {
        Campaign campaign = campaignRepository
                .findById(campaignId)
                .orElseThrow(() -> new BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));
        if (!campaign.isActiveAt(now)) {
            throw new BusinessException(CampaignErrorCode.CAMPAIGN_INACTIVE);
        }
        return campaign;
    }

    // 체크인과 같게, 목표 버전을 읽을 수 없거나 이미 설치한 버전이면 배포하지 않는다
    private boolean isDeliverable(Campaign campaign, ArtifactSummary artifact, TargetVehicle vehicle) {
        return toTargetCampaign(campaign, artifact)
                .filter(target -> !target.isInstalledOn(vehicle.currentVersion()))
                .isPresent();
    }

    private Optional<TargetCampaign> toTargetCampaign(Campaign campaign) {
        return toTargetCampaign(campaign, artifactQueryService.getById(campaign.artifactId()));
    }

    private Optional<TargetCampaign> toTargetCampaign(Campaign campaign, ArtifactSummary artifact) {
        return DottedVersion.parse(artifact.version()).map(version -> new TargetCampaign(campaign.id(), version));
    }
}
