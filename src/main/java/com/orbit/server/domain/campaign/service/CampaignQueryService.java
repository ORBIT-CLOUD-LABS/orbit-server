package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.artifact.service.ArtifactQueryService;
import com.orbit.server.domain.artifact.service.ArtifactSummary;
import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.entity.DottedVersion;
import com.orbit.server.domain.campaign.entity.TargetCampaign;
import com.orbit.server.domain.campaign.entity.TargetVehicle;
import com.orbit.server.domain.campaign.repository.CampaignRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차량이 업데이트할 캠페인을 고른다. 다른 컨텍스트(체크인)는 캠페인을 직접 읽지 않고 이 서비스로만 대상 여부를 묻는다. (VEHICLE-POL-001)
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

    private Optional<TargetCampaign> toTargetCampaign(Campaign campaign) {
        ArtifactSummary artifact = artifactQueryService.getById(campaign.artifactId());
        return DottedVersion.parse(artifact.version()).map(version -> new TargetCampaign(campaign.id(), version));
    }
}
