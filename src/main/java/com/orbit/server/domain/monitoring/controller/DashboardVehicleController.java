package com.orbit.server.domain.monitoring.controller;

import com.orbit.server.domain.monitoring.dto.response.VehicleStatusSnapshotResponse;
import com.orbit.server.domain.monitoring.service.VehicleStatusQueryService;
import com.orbit.server.domain.monitoring.service.VehicleStatusStreamService;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 대시보드용 차량 상태 API. 관리자 토큰 인증은 SecurityConfig 가 경로 단위로 적용한다. */
@RestController
@RequestMapping("/api/v1/dashboard/vehicles")
@RequiredArgsConstructor
public class DashboardVehicleController {

    private static final String LAST_EVENT_ID = "Last-Event-ID";

    private final VehicleStatusQueryService vehicleStatusQueryService;
    private final VehicleStatusStreamService vehicleStatusStreamService;

    /**
     * 전체 차량 상태 스냅샷을 조회한다. 대시보드는 SSE 를 먼저 연결한 뒤 이 API 를 부른다.
     * (MONITORING-REQ-004 / MONITORING-POL-001, MONITORING-POL-003)
     *
     * @return 200 과 기준 시각, vehicleId 오름차순의 전체 차량 상태
     */
    @GetMapping
    public VehicleStatusSnapshotResponse getSnapshot() {
        return VehicleStatusSnapshotResponse.from(vehicleStatusQueryService.getSnapshot());
    }

    /**
     * 차량 상태 변경분 스트림을 연다. 변경이 있을 때만 {@code event: vehicles} 를 보내고 이벤트 id 는 변경분 기준 시각이다.
     * (MONITORING-REQ-001 / MONITORING-POL-001)
     *
     * <p>{@code EventSource} 는 헤더를 붙이지 못하므로 대시보드는 fetch 기반 SSE 로 관리자 토큰을 {@code Authorization} 헤더에
     * 담아 연결한다.
     *
     * @param lastEventId 재연결 때 마지막으로 받은 이벤트 id. 처음 연결이면 없다
     * @return 200 과 {@code text/event-stream}. 재연결이면 놓친 변경분을 먼저 보낸다
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader(name = LAST_EVENT_ID, required = false) Instant lastEventId) {
        return vehicleStatusStreamService.subscribe(Optional.ofNullable(lastEventId));
    }
}
