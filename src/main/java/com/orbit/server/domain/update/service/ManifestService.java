package com.orbit.server.domain.update.service;

import com.orbit.server.domain.artifact.service.ArtifactSummary;
import com.orbit.server.domain.campaign.service.CampaignQueryService;
import com.orbit.server.domain.campaign.service.TargetVehicleCriteria;
import com.orbit.server.domain.vehicle.service.VehicleProfile;
import com.orbit.server.domain.vehicle.service.VehicleQueryService;
import com.orbit.server.global.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 업데이트 대상 차량에 매니페스트를 발급한다. 요청 시점에 대상 여부를 다시 확인한 뒤 파일 정보와 가까운 Edge 의 다운로드 URL 을 내려준다.
 *
 * <p>다른 컨텍스트인 차량과 캠페인은 각 컨텍스트의 서비스로만 읽는다. 기록하는 것은 차량 상태의 배정 Edge 뿐이다.
 */
@Service
@RequiredArgsConstructor
public class ManifestService {

    private final VehicleQueryService vehicleQueryService;
    private final VehicleStateService vehicleStateService;
    private final CampaignQueryService campaignQueryService;
    private final EdgeAssigner edgeAssigner;
    private final Clock clock;

    /**
     * 차량 조회 → 마지막 체크인 때의 현재 버전 조회 → 요청한 캠페인의 대상인지 판정 → Edge 배정과 다운로드 URL 발급 순서로 처리한다.
     *
     * <p>체크인 이후 더 높은 버전의 캠페인이 등록됐더라도 요청한 캠페인으로 발급한다. 새 캠페인은 다음 체크인에서 반영된다. 대상 판정이 끝난
     * 뒤에 Edge 를 배정하므로 대상이 아닌 차량에는 배정 Edge 가 기록되지 않는다.
     *
     * @param vehicleId 요청 경로의 차량 식별자
     * @param campaignId 요청 경로의 캠페인 ID
     * @return 캠페인 ID, 목표 버전, 파일 크기·해시, 배정 Edge 와 다운로드·예비 URL
     * @throws BusinessException 캠페인이 없으면 CAMPAIGN_NOT_FOUND, 비활성이거나 적용 기간 밖이면 CAMPAIGN_INACTIVE, 대상이 아니면
     *     NOT_UPDATE_TARGET, 정상인 Edge 가 없으면 NO_AVAILABLE_EDGE
     */
    // 여러 컨텍스트를 차례로 조회하고 배정 Edge 를 기록하므로 한 커넥션·한 스냅샷에서 처리하도록 묶는다.
    // 배정 Edge 기록(UPDATE)은 마지막 단계라 차량 상태 행을 잠그는 시간은 그 뒤 커밋까지뿐이다
    @Transactional
    public ManifestResult issue(String vehicleId, Long campaignId) {
        VehicleProfile vehicle = vehicleQueryService.getProfile(vehicleId);
        ManifestVehicleState state = vehicleStateService.getManifestState(vehicle.vehicleId());
        TargetVehicleCriteria target = new TargetVehicleCriteria(
                vehicle.model(),
                vehicle.hwVersion(),
                vehicle.region(),
                state.currentVersion().value());
        // 적용 기간 판정과 URL 만료 시각이 같은 시각을 기준으로 하도록 한 번만 읽는다
        Instant now = clock.instant();
        ArtifactSummary artifact = campaignQueryService.getTargetArtifact(campaignId, target, now);
        EdgeAssignment assignment = edgeAssigner.assign(state, artifact.path(), now);
        return ManifestResult.of(campaignId, artifact, assignment);
    }
}
