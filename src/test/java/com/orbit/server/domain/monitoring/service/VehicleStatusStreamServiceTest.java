package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class VehicleStatusStreamServiceTest {

    private static final Duration OVERLAP = Duration.ofSeconds(10);
    private static final DashboardStreamProperties PROPERTIES =
            new DashboardStreamProperties(Duration.ofSeconds(1), Duration.ofSeconds(15), OVERLAP);
    private static final Instant LAST_EVENT_ID = Instant.parse("2026-10-01T12:00:00Z");
    private static final Instant AS_OF = Instant.parse("2026-10-01T12:00:05Z");
    private static final VehicleStatus VEHICLE =
            new VehicleStatus("vehicle-001", "model-a", "1.0.0", AS_OF, AS_OF, null, null, null);

    private final VehicleStatusQuery query = mock(VehicleStatusQuery.class);
    private final VehicleStatusSubscribers subscribers = mock(VehicleStatusSubscribers.class);
    private final VehicleStatusStreamService service = new VehicleStatusStreamService(query, subscribers, PROPERTIES);

    @Test
    @DisplayName("MONITORING-REQ-001: 처음 연결하면 구독자로 더하고 변경분을 따로 조회하지 않는다")
    void subscribesWithoutCatchUp() {
        SseEmitter emitter = service.subscribe(Optional.empty());

        verify(subscribers).add(emitter);
        verifyNoInteractions(query);
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 재연결하면 Last-Event-ID 보다 겹침 시간만큼 앞선 시각부터 조회해 그 연결에만 보낸다")
    void catchesUpFromLastEventIdMinusOverlap() {
        VehicleStatusChanges missed = new VehicleStatusChanges(AS_OF, List.of(VEHICLE));
        given(query.findChangesSince(LAST_EVENT_ID.minus(OVERLAP))).willReturn(missed);

        SseEmitter emitter = service.subscribe(Optional.of(LAST_EVENT_ID));

        verify(subscribers).sendTo(emitter, missed);
        verify(subscribers, never()).broadcast(any());
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 재연결했지만 놓친 변경이 없으면 보내지 않는다")
    void doesNotSendWithoutMissedChanges() {
        given(query.findChangesSince(any())).willReturn(new VehicleStatusChanges(AS_OF, List.of()));

        service.subscribe(Optional.of(LAST_EVENT_ID));

        verify(subscribers, never()).sendTo(any(), any());
    }

    @Test
    @DisplayName("MONITORING-POL-001: 따라잡는 사이의 변경을 놓치지 않도록 구독자로 먼저 더한 뒤 조회한다")
    void subscribesBeforeCatchUp() {
        given(query.findChangesSince(any())).willReturn(new VehicleStatusChanges(AS_OF, List.of(VEHICLE)));

        SseEmitter emitter = service.subscribe(Optional.of(LAST_EVENT_ID));

        InOrder order = inOrder(subscribers, query);
        order.verify(subscribers).add(emitter);
        order.verify(query).findChangesSince(any());
        order.verify(subscribers).sendTo(any(), any());
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 따라잡기 조회가 실패하면 구독자에서 빼고 실패를 알린다")
    void removesSubscriberWhenCatchUpFails() {
        given(query.findChangesSince(any())).willThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.subscribe(Optional.of(LAST_EVENT_ID)))
                .isInstanceOf(IllegalStateException.class);

        verify(subscribers).remove(any(SseEmitter.class));
    }
}
