package com.orbit.server.domain.update.service;

import com.orbit.server.domain.campaign.entity.TargetVehicle;
import com.orbit.server.domain.campaign.service.CampaignQueryService;
import com.orbit.server.domain.vehicle.service.VehicleProfile;
import com.orbit.server.domain.vehicle.service.VehicleQueryService;
import com.orbit.server.global.error.InvalidValueException;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차량의 주기적인 체크인을 처리한다. 현재 상태와 업데이트 결과를 기록하고 업데이트할 캠페인이 있는지 알려 준다. (VEHICLE-REQ-001,
 * UPDATE-REQ-001 / VEHICLE-POL-001, UPDATE-POL-001)
 *
 * <p>다른 컨텍스트인 차량과 캠페인은 각 컨텍스트의 서비스로만 읽는다. 차량({@code vehicle})에는 쓰지 않는다.
 */
@Service
@RequiredArgsConstructor
public class CheckInService {

    // 대시보드 SSE 는 updated_at 이 이전 기준 시각보다 겹침 시간만큼 이른 변경까지 다시 읽는다.
    // 체크인이 겹침 시간보다 늦게 커밋되면 그 변경을 놓치므로 겹침 시간(orbit.dashboard.stream.overlap)보다 짧게 둔다.
    // Spring 트랜잭션 타임아웃은 SQL 문 단위로 확인해 커밋 지연까지는 막지 못하므로 겹침 시간은 이보다 넉넉히 잡는다.
    // 타임아웃으로 롤백된 체크인은 차량이 다음 주기에 다시 보내므로 데이터를 잃지 않는다
    private static final int TRANSACTION_TIMEOUT_SECONDS = 3;

    private final VehicleQueryService vehicleQueryService;
    private final VehicleStateService vehicleStateService;
    private final UpdateResultService updateResultService;
    private final CampaignQueryService campaignQueryService;
    private final CheckInProperties checkInProperties;
    private final Clock clock;

    /**
     * 차량 조회 → 현재 버전·마지막 체크인 시각 기록 → 업데이트 결과 기록 → 대상 캠페인 판정 순서로 처리한다.
     *
     * @param command 체크인 요청
     * @return 대상 캠페인과 다음 체크인까지의 시간
     * @throws InvalidValueException 현재 버전이 비어 있거나 50자를 넘는 경우, 업데이트 결과의 형식이 틀린 경우
     */
    // 상태와 결과를 한 트랜잭션에서 기록해 결과가 들어오면 vehicle_state.updated_at 도 함께 바뀌어 SSE 변경분에 잡힌다.
    // 중간에 실패하면 모두 되돌려, 차량이 다음 주기에 다시 보낸 체크인으로 한 번에 반영되게 한다
    @Transactional(timeout = TRANSACTION_TIMEOUT_SECONDS)
    public CheckInResult checkIn(CheckInCommand command) {
        VehicleProfile vehicle = vehicleQueryService.getProfile(command.vehicleId());
        vehicleStateService.recordCheckIn(vehicle.vehicleId(), command.currentVersion());
        command.lastUpdate().ifPresent(report -> updateResultService.record(vehicle.vehicleId(), report));
        TargetVehicle target =
                new TargetVehicle(vehicle.model(), vehicle.hwVersion(), vehicle.region(), command.currentVersion());
        Optional<Long> campaignId = campaignQueryService.findTargetCampaignId(target, clock.instant());
        return new CheckInResult(campaignId, checkInProperties.intervalSeconds());
    }
}
