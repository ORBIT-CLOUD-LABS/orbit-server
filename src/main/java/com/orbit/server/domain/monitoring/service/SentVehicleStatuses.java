package com.orbit.server.domain.monitoring.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 겹침 조회 구간에서 이미 보낸 차량 상태. 변경분은 늦게 커밋된 변경을 놓치지 않도록 이전 기준 시각보다 앞당겨 겹쳐 조회하므로, 같은 상태가
 * 겹침 시간 동안 매 주기 다시 잡힌다. 이를 걸러 실제로 바뀐 것이 있을 때만 보내게 한다. (MONITORING-REQ-001 / MONITORING-POL-001)
 *
 * <p>pod 의 스케줄러 스레드 하나만 쓰므로 동기화하지 않는다.
 */
public class SentVehicleStatuses {

    // updatedAt 만으로 구분하면 같은 밀리초에 두 번 바뀐 상태를 놓치므로 상태 전체로 구분한다
    private final Set<VehicleStatus> sent = new HashSet<>();

    /**
     * 변경분에서 아직 보내지 않은 상태만 남기고, 남긴 상태를 보낸 것으로 기억한다. 다음 조회 범위를 벗어나 다시 잡히지 않을 기록은 지운다.
     *
     * @param since 이번 변경분을 조회한 하한 시각. 상태 변경 시각이 이 시각 이하인 기록은 다시 잡히지 않으므로 지운다
     * @param changes 이번에 조회한 변경분
     * @return 기준 시각은 그대로 두고 아직 보내지 않은 상태만 담은 변경분
     */
    public VehicleStatusChanges keepUnsent(Instant since, VehicleStatusChanges changes) {
        sent.removeIf(status -> !status.updatedAt().isAfter(since));
        List<VehicleStatus> unsent =
                changes.vehicles().stream().filter(sent::add).toList();
        return new VehicleStatusChanges(changes.asOf(), unsent);
    }
}
