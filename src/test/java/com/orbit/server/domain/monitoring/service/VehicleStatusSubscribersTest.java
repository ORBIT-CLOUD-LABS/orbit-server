package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

class VehicleStatusSubscribersTest {

    private static final Instant AS_OF = Instant.parse("2026-10-01T12:00:00.123Z");
    private static final VehicleStatus VEHICLE = VehicleStatusFixture.withoutResult("vehicle-001", AS_OF);
    private static final VehicleStatusChanges CHANGES = new VehicleStatusChanges(AS_OF, List.of(VEHICLE));
    // SSE 는 응답 DTO 를 거치지 않고 차량 상태를 그대로 JSON 으로 내보내므로, 직렬화한 모양을 명세와 맞춰 본다
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final VehicleStatusSubscribers subscribers = new VehicleStatusSubscribers();

    @Test
    @DisplayName("MONITORING-REQ-001: 변경분을 모든 구독자에게 기준 시각을 id 로 한 vehicles 이벤트로 보낸다")
    void broadcastsVehiclesEventToAllSubscribers() {
        RecordingEmitter first = subscribe();
        RecordingEmitter second = subscribe();

        subscribers.broadcast(CHANGES);

        assertThat(List.of(first, second)).allSatisfy(emitter -> {
            assertThat(emitter.texts)
                    .singleElement()
                    .satisfies(text ->
                            assertThat(text).contains("event:vehicles\n", "id:2026-10-01T12:00:00.123Z\n", "data:"));
            assertThat(emitter.data).containsExactly(List.of(VEHICLE));
        });
    }

    @Test
    @DisplayName("MONITORING-REQ-001: vehicles 이벤트의 차량은 스냅샷과 같은 형식으로 위치·배정 Edge·다운로드 Edge 를 담는다")
    void sendsVehicleWithLocationAndEdges() {
        VehicleStatus located = new VehicleStatus(
                "vehicle-001",
                "model-a",
                "1.0.0",
                AS_OF,
                AS_OF,
                7L,
                "FAILED",
                "SIGNATURE_INVALID",
                new VehicleLastLocation(37.5665, 126.978, AS_OF),
                "edge-seoul",
                "edge-daejeon");
        RecordingEmitter emitter = subscribe();

        subscribers.broadcast(new VehicleStatusChanges(AS_OF, List.of(located)));

        assertThat(emitter.data)
                .singleElement()
                .satisfies(data -> assertThat(JSON_MAPPER.writeValueAsString(data))
                        .isEqualTo("[{\"vehicleId\":\"vehicle-001\",\"model\":\"model-a\",\"currentVersion\":\"1.0.0\","
                                + "\"lastSeenAt\":\"2026-10-01T12:00:00.123Z\",\"updatedAt\":\"2026-10-01T12:00:00.123Z\","
                                + "\"campaignId\":7,\"lastResult\":\"FAILED\",\"failureReason\":\"SIGNATURE_INVALID\","
                                + "\"location\":{\"latitude\":37.5665,\"longitude\":126.978,"
                                + "\"updatedAt\":\"2026-10-01T12:00:00.123Z\"},"
                                + "\"assignedEdgeId\":\"edge-seoul\",\"downloadedEdgeId\":\"edge-daejeon\"}]"));
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 위치·Edge 가 없는 차량은 vehicles 이벤트에 해당 필드를 null 로 담는다")
    void sendsVehicleWithoutLocationAndEdges() {
        RecordingEmitter emitter = subscribe();

        subscribers.broadcast(CHANGES);

        assertThat(emitter.data)
                .singleElement()
                .satisfies(data -> assertThat(JSON_MAPPER.writeValueAsString(data))
                        .contains("\"location\":null", "\"assignedEdgeId\":null", "\"downloadedEdgeId\":null"));
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 따라잡을 변경분은 지정한 연결에만 vehicles 이벤트로 보낸다")
    void sendsVehiclesEventToOneSubscriber() {
        RecordingEmitter target = subscribe();
        RecordingEmitter other = subscribe();

        subscribers.sendTo(target, CHANGES);

        assertThat(target.texts)
                .singleElement()
                .satisfies(text -> assertThat(text).contains("event:vehicles\n", "id:2026-10-01T12:00:00.123Z\n"));
        assertThat(target.data).containsExactly(List.of(VEHICLE));
        assertThat(other.texts).isEmpty();
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 뺀 구독자에게는 보내지 않는다")
    void removesSubscriber() {
        RecordingEmitter emitter = subscribe();

        subscribers.remove(emitter);
        subscribers.broadcast(CHANGES);

        assertThat(emitter.texts).isEmpty();
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 모든 구독자에게 keep-alive 주석을 보낸다")
    void sendsKeepAliveCommentToAllSubscribers() {
        RecordingEmitter first = subscribe();
        RecordingEmitter second = subscribe();

        subscribers.sendKeepAlive();

        assertThat(List.of(first, second))
                .allSatisfy(emitter -> assertThat(emitter.texts).containsExactly(":keep-alive\n\n"));
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 연결이 끝난 구독자에게는 보내지 않는다")
    void removesCompletedSubscriber() {
        RecordingEmitter emitter = subscribe();

        emitter.completionCallback.run();
        subscribers.broadcast(CHANGES);

        assertThat(emitter.texts).isEmpty();
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 시간이 다 된 구독자에게는 보내지 않는다")
    void removesTimedOutSubscriber() {
        RecordingEmitter emitter = subscribe();

        emitter.timeoutCallback.run();
        subscribers.broadcast(CHANGES);

        assertThat(emitter.texts).isEmpty();
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 오류가 난 구독자에게는 보내지 않는다")
    void removesFailedSubscriber() {
        RecordingEmitter emitter = subscribe();

        emitter.errorCallback.accept(new IOException("broken pipe"));
        subscribers.broadcast(CHANGES);

        assertThat(emitter.texts).isEmpty();
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 보내지 못한 구독자는 빼고 다른 구독자에게는 계속 보낸다")
    void removesSubscriberOnSendFailure() {
        RecordingEmitter broken = subscribe();
        broken.failOnSend = true;
        RecordingEmitter healthy = subscribe();

        subscribers.broadcast(CHANGES);
        subscribers.sendKeepAlive();

        assertThat(broken.sendAttempts).isEqualTo(1);
        assertThat(healthy.texts).hasSize(2);
    }

    private RecordingEmitter subscribe() {
        RecordingEmitter emitter = new RecordingEmitter();
        subscribers.add(emitter);
        return emitter;
    }

    // 실제 응답 없이 보낸 내용과 등록한 콜백을 기록한다
    private static class RecordingEmitter extends SseEmitter {

        private final List<String> texts = new ArrayList<>();
        private final List<Object> data = new ArrayList<>();
        private Runnable completionCallback;
        private Runnable timeoutCallback;
        private Consumer<Throwable> errorCallback;
        private boolean failOnSend;
        private int sendAttempts;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            sendAttempts++;
            if (failOnSend) {
                throw new IOException("broken pipe");
            }
            List<Object> parts =
                    builder.build().stream().map(DataWithMediaType::getData).toList();
            texts.add(parts.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.joining()));
            parts.stream().filter(part -> !(part instanceof String)).forEach(data::add);
        }

        @Override
        public void onCompletion(Runnable callback) {
            completionCallback = callback;
        }

        @Override
        public void onTimeout(Runnable callback) {
            timeoutCallback = callback;
        }

        @Override
        public void onError(Consumer<Throwable> callback) {
            errorCallback = callback;
        }
    }
}
