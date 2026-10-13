package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class VehicleStatusBroadcasterTest {

    private static final Duration OVERLAP = Duration.ofSeconds(10);
    private static final DashboardStreamProperties PROPERTIES =
            new DashboardStreamProperties(Duration.ofSeconds(1), Duration.ofSeconds(15), OVERLAP);
    private static final Instant STARTED_AT = Instant.parse("2026-10-01T12:00:00Z");
    private static final Instant FIRST_AS_OF = Instant.parse("2026-10-01T12:00:01Z");
    private static final Instant SECOND_AS_OF = Instant.parse("2026-10-01T12:00:02Z");
    private static final VehicleStatus VEHICLE =
            new VehicleStatus("vehicle-001", "model-a", "1.0.0", FIRST_AS_OF, FIRST_AS_OF, null, null, null);

    private final VehicleStatusQuery query = mock(VehicleStatusQuery.class);
    private final VehicleStatusSubscribers subscribers = mock(VehicleStatusSubscribers.class);
    private final VehicleStatusBroadcaster broadcaster =
            new VehicleStatusBroadcaster(query, subscribers, PROPERTIES, Clock.fixed(STARTED_AT, ZoneOffset.UTC));

    @Test
    @DisplayName("MONITORING-POL-001: 연결 수와 관계없이 주기마다 변경분을 한 번만 조회한다")
    void queriesOncePerPeriodRegardlessOfSubscribers() {
        VehicleStatusSubscribers realSubscribers = new VehicleStatusSubscribers();
        List.of(new SseEmitter(), new SseEmitter(), new SseEmitter()).forEach(realSubscribers::add);
        VehicleStatusBroadcaster sharedBroadcaster = new VehicleStatusBroadcaster(
                query, realSubscribers, PROPERTIES, Clock.fixed(STARTED_AT, ZoneOffset.UTC));
        given(query.findChangesSince(any())).willReturn(changes(FIRST_AS_OF, VEHICLE), changes(SECOND_AS_OF));

        sharedBroadcaster.broadcastChanges();
        sharedBroadcaster.broadcastChanges();

        verify(query, times(2)).findChangesSince(any());
    }

    @Test
    @DisplayName("MONITORING-POL-001: 첫 주기는 시작 시각보다 겹침 시간만큼 앞선 시각부터 조회한다")
    void firstPeriodQueriesFromStartMinusOverlap() {
        given(query.findChangesSince(any())).willReturn(changes(FIRST_AS_OF));

        broadcaster.broadcastChanges();

        verify(query).findChangesSince(STARTED_AT.minus(OVERLAP));
    }

    @Test
    @DisplayName("MONITORING-POL-001: 다음 주기는 이전 기준 시각보다 겹침 시간만큼 앞선 시각부터 조회한다")
    void nextPeriodQueriesFromPreviousAsOfMinusOverlap() {
        given(query.findChangesSince(any())).willReturn(changes(FIRST_AS_OF), changes(SECOND_AS_OF));

        broadcaster.broadcastChanges();
        broadcaster.broadcastChanges();

        verify(query).findChangesSince(FIRST_AS_OF.minus(OVERLAP));
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 변경이 있으면 구독자 전체에 보낸다")
    void broadcastsChanges() {
        given(query.findChangesSince(any())).willReturn(changes(FIRST_AS_OF, VEHICLE));

        broadcaster.broadcastChanges();

        verify(subscribers).broadcast(changes(FIRST_AS_OF, VEHICLE));
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 변경이 없으면 보내지 않는다")
    void doesNotBroadcastWithoutChanges() {
        given(query.findChangesSince(any())).willReturn(changes(FIRST_AS_OF));

        broadcaster.broadcastChanges();

        verify(subscribers, never()).broadcast(any());
    }

    @Test
    @DisplayName("MONITORING-POL-001: 겹침 조회로 다시 잡힌 상태는 다시 보내지 않는다")
    void doesNotBroadcastOverlappedStatusAgain() {
        given(query.findChangesSince(any())).willReturn(changes(FIRST_AS_OF, VEHICLE), changes(SECOND_AS_OF, VEHICLE));

        broadcaster.broadcastChanges();
        broadcaster.broadcastChanges();

        verify(subscribers, times(1)).broadcast(any());
    }

    @Test
    @DisplayName("MONITORING-POL-001: 조회가 실패하면 다음 주기에 같은 구간부터 다시 조회한다")
    void retriesSameRangeAfterQueryFailure() {
        given(query.findChangesSince(any()))
                .willThrow(new IllegalStateException("db down"))
                .willReturn(changes(FIRST_AS_OF));

        // 스케줄러는 예외를 기록하고 다음 주기를 돌린다
        assertThatThrownBy(broadcaster::broadcastChanges).isInstanceOf(IllegalStateException.class);
        broadcaster.broadcastChanges();

        verify(query, times(2)).findChangesSince(STARTED_AT.minus(OVERLAP));
    }

    private VehicleStatusChanges changes(Instant asOf, VehicleStatus... statuses) {
        return new VehicleStatusChanges(asOf, List.of(statuses));
    }
}
