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

class VehicleStatusSubscribersTest {

    private static final Instant AS_OF = Instant.parse("2026-10-01T12:00:00.123Z");
    private static final VehicleStatus VEHICLE =
            new VehicleStatus("vehicle-001", "model-a", "1.0.0", AS_OF, AS_OF, null, null, null);
    private static final VehicleStatusChanges CHANGES = new VehicleStatusChanges(AS_OF, List.of(VEHICLE));

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
