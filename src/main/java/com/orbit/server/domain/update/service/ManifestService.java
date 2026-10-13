package com.orbit.server.domain.update.service;

import com.orbit.server.domain.artifact.service.ArtifactSummary;
import com.orbit.server.domain.campaign.entity.TargetVehicle;
import com.orbit.server.domain.campaign.service.CampaignQueryService;
import com.orbit.server.domain.update.entity.SoftwareVersion;
import com.orbit.server.domain.vehicle.service.VehicleProfile;
import com.orbit.server.domain.vehicle.service.VehicleQueryService;
import com.orbit.server.global.error.BusinessException;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 업데이트 대상 차량에 매니페스트를 발급한다. 요청 시점에 대상 여부를 다시 확인한 뒤 파일 정보와 다운로드 URL 을 내려준다.
 * (MANIFEST-REQ-001 / MANIFEST-POL-001)
 *
 * <p>다른 컨텍스트인 차량과 캠페인은 각 컨텍스트의 서비스로만 읽는다. 아무것도 기록하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ManifestService {

    private final VehicleQueryService vehicleQueryService;
    private final VehicleStateService vehicleStateService;
    private final CampaignQueryService campaignQueryService;
    private final DownloadUrlIssuer downloadUrlIssuer;
    private final Clock clock;

    /**
     * 차량 조회 → 마지막 체크인 때의 현재 버전 조회 → 요청한 캠페인의 대상인지 판정 → 다운로드 URL 발급 순서로 처리한다.
     *
     * <p>체크인 이후 더 높은 버전의 캠페인이 등록됐더라도 요청한 캠페인으로 발급한다. 새 캠페인은 다음 체크인에서 반영된다.
     *
     * @param vehicleId 요청 경로의 차량 식별자
     * @param campaignId 요청 경로의 캠페인 ID
     * @return 캠페인 ID, 목표 버전, 파일 크기·해시, 다운로드 URL
     * @throws BusinessException 캠페인이 없으면 CAMPAIGN_NOT_FOUND, 비활성이거나 적용 기간 밖이면 CAMPAIGN_INACTIVE, 대상이 아니면
     *     NOT_UPDATE_TARGET
     */
    // 여러 컨텍스트를 차례로 조회하므로 한 커넥션·한 스냅샷에서 판정하도록 묶는다
    @Transactional(readOnly = true)
    public ManifestResult issue(String vehicleId, Long campaignId) {
        VehicleProfile vehicle = vehicleQueryService.getProfile(vehicleId);
        SoftwareVersion currentVersion = vehicleStateService.getCurrentVersion(vehicle.vehicleId());
        TargetVehicle target =
                new TargetVehicle(vehicle.model(), vehicle.hwVersion(), vehicle.region(), currentVersion.value());
        ArtifactSummary artifact = campaignQueryService.getTargetArtifact(campaignId, target, clock.instant());
        return ManifestResult.of(campaignId, artifact, downloadUrlIssuer.issue(artifact.path()));
    }
}
