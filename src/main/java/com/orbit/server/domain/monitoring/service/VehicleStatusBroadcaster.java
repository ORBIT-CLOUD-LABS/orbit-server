package com.orbit.server.domain.monitoring.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * 주기마다 차량 상태 변경분을 한 번 조회해 이 pod 에 연결된 대시보드 전체에 보낸다. (MONITORING-REQ-001 / MONITORING-POL-001)
 *
 * <p>연결마다 조회하면 대시보드 수만큼 DB 부하가 늘어 SSE 를 쓰는 이점이 사라지므로 pod 당 하나의 커서로 한 번만 조회한다. 스케줄러 스레드
 * 하나에서만 호출하므로 커서와 보낸 상태를 동기화하지 않는다.
 */
@Component
public class VehicleStatusBroadcaster {

    private final VehicleStatusQuery vehicleStatusQuery;
    private final VehicleStatusSubscribers subscribers;
    private final Duration overlap;
    private final SentVehicleStatuses sentStatuses = new SentVehicleStatuses();
    private Instant cursor;

    /**
     * @param vehicleStatusQuery 변경분 조회
     * @param subscribers 이 pod 에 연결된 대시보드
     * @param properties 겹침 시간 설정
     * @param clock 첫 커서를 정할 시계
     */
    // 첫 커서는 DB 가 아닌 앱 시각으로 정한다. 시작 전 변경은 대시보드가 스냅샷으로 받으므로 필요 없고,
    // 두 시계의 차이는 겹침 시간이 덮는다. 다음 커서부터는 DB 시각을 쓴다
    public VehicleStatusBroadcaster(
            VehicleStatusQuery vehicleStatusQuery,
            VehicleStatusSubscribers subscribers,
            DashboardStreamProperties properties,
            Clock clock) {
        this.vehicleStatusQuery = vehicleStatusQuery;
        this.subscribers = subscribers;
        this.overlap = properties.overlap();
        this.cursor = clock.instant();
    }

    /**
     * 이전 기준 시각보다 겹침 시간만큼 앞선 시각부터 변경분을 조회해, 아직 보내지 않은 상태가 있을 때만 {@code event: vehicles} 로 보낸다.
     *
     * <p>조회가 실패하면 커서를 옮기지 않으므로 다음 주기에 같은 구간부터 다시 조회한다.
     */
    // updated_at 은 커밋이 아니라 UPDATE 실행 시각이라, 조회 때 커밋 전이던 체크인이 커서보다 이른 updated_at 으로 늦게 커밋될 수 있다.
    // 그래서 커서보다 앞선 구간을 겹쳐 다시 읽고, 겹쳐 다시 잡힌 상태는 보낸 상태로 거른다
    public void broadcastChanges() {
        Instant since = cursor.minus(overlap);
        VehicleStatusChanges unsent = sentStatuses.keepUnsent(since, vehicleStatusQuery.findChangesSince(since));
        cursor = unsent.asOf();
        if (unsent.vehicles().isEmpty()) {
            return;
        }
        subscribers.broadcast(unsent);
    }
}
