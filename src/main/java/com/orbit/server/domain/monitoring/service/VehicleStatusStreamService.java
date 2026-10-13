package com.orbit.server.domain.monitoring.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 대시보드의 차량 상태 SSE 구독을 연다. 변경분은 pod 의 스케줄러가 구독자 전체에 보내고, 이 서비스는 재연결한 대시보드가 놓친 변경분만
 * 그 연결에 한 번 보낸다. (MONITORING-REQ-001 / MONITORING-POL-001)
 */
@Service
public class VehicleStatusStreamService {

    // 대시보드가 닫을 때까지 연결을 유지한다. 끊긴 연결은 keep-alive 전송이 실패할 때 정리된다
    private static final long NO_TIMEOUT = 0L;

    private final VehicleStatusQuery vehicleStatusQuery;
    private final VehicleStatusSubscribers subscribers;
    private final Duration overlap;

    /**
     * @param vehicleStatusQuery 놓친 변경분 조회
     * @param subscribers 이 pod 에 연결된 대시보드
     * @param properties 겹침 시간 설정
     */
    public VehicleStatusStreamService(
            VehicleStatusQuery vehicleStatusQuery,
            VehicleStatusSubscribers subscribers,
            DashboardStreamProperties properties) {
        this.vehicleStatusQuery = vehicleStatusQuery;
        this.subscribers = subscribers;
        this.overlap = properties.overlap();
    }

    /**
     * 연결을 구독자로 더하고, 재연결이면 마지막으로 받은 이벤트 이후의 변경분을 이 연결에만 보낸다.
     *
     * @param lastEventId 대시보드가 마지막으로 받은 이벤트 id(변경분 기준 시각). 처음 연결이면 비어 있다
     * @return 대시보드와 연결된 SSE
     */
    // 따라잡는 사이 스케줄러가 보낸 변경을 놓치지 않도록 구독자로 먼저 더한 뒤 조회한다.
    // 두 쪽에서 같은 변경을 받아도 대시보드는 updatedAt 이 늦은 쪽을 쓰므로 문제없다
    public SseEmitter subscribe(Optional<Instant> lastEventId) {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        subscribers.add(emitter);
        lastEventId.ifPresent(eventId -> catchUp(emitter, eventId));
        return emitter;
    }

    private void catchUp(SseEmitter emitter, Instant lastEventId) {
        VehicleStatusChanges missed = findMissed(emitter, lastEventId);
        if (missed.vehicles().isEmpty()) {
            return;
        }
        subscribers.sendTo(emitter, missed);
    }

    // 스케줄러와 같은 이유로 늦게 커밋된 변경까지 받도록 겹쳐 조회한다.
    // 조회가 실패하면 연결이 시작되지 않아 종료 콜백이 오지 않으므로 구독자에서 직접 뺀다
    private VehicleStatusChanges findMissed(SseEmitter emitter, Instant lastEventId) {
        try {
            return vehicleStatusQuery.findChangesSince(lastEventId.minus(overlap));
        } catch (RuntimeException e) {
            subscribers.remove(emitter);
            throw e;
        }
    }
}
