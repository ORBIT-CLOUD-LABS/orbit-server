package com.orbit.server.domain.monitoring.service;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

/**
 * 이 pod 에 연결된 대시보드 SSE 구독자 목록. 변경분 조회는 pod 당 한 번만 하고 그 결과를 여기 모인 연결 전체에 보낸다.
 * (MONITORING-REQ-001 / MONITORING-POL-001)
 *
 * <p>스케줄러 스레드가 보내는 동안 요청 스레드가 연결을 더하고 빼므로 동시성 집합에 담는다.
 */
@Slf4j
@Component
public class VehicleStatusSubscribers {

    private static final String VEHICLES_EVENT = "vehicles";
    private static final String KEEP_ALIVE_COMMENT = "keep-alive";

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    /**
     * 연결을 구독자로 더한다. 연결이 끝나거나 시간이 다 되거나 오류가 나면 목록에서 뺀다.
     *
     * @param emitter 대시보드와 연결된 SSE
     */
    public void add(SseEmitter emitter) {
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        emitters.add(emitter);
    }

    /**
     * 변경분을 모든 구독자에게 {@code event: vehicles} 로 보낸다. 이벤트 id 는 변경분의 기준 시각이라 재연결 때 {@code Last-Event-ID}
     * 로 돌아온다.
     *
     * @param changes 보낼 변경분
     */
    public void broadcast(VehicleStatusChanges changes) {
        // 이벤트 빌더는 만들 때 내용을 덧붙여 바꾸므로 연결마다 새로 만든다
        emitters.forEach(emitter -> send(emitter, vehiclesEvent(changes)));
    }

    /** 프록시·로드밸런서가 유휴 연결을 끊지 않도록 모든 구독자에게 {@code : keep-alive} 주석을 보낸다. */
    public void sendKeepAlive() {
        emitters.forEach(emitter -> send(emitter, SseEmitter.event().comment(KEEP_ALIVE_COMMENT)));
    }

    private SseEventBuilder vehiclesEvent(VehicleStatusChanges changes) {
        return SseEmitter.event()
                .name(VEHICLES_EVENT)
                .id(changes.asOf().toString())
                .data(changes.vehicles(), MediaType.APPLICATION_JSON);
    }

    // 보내지 못한 연결은 이미 끊긴 것이다. 컨테이너가 오류 콜백으로 정리하지만, 그 전에 다음 전송에서 다시 시도하지 않도록 바로 뺀다.
    // 한 연결의 실패가 다른 구독자 전송을 막지 않도록 예외를 밖으로 던지지 않는다
    private void send(SseEmitter emitter, SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            emitters.remove(emitter);
            log.debug("대시보드 SSE 연결에 보내지 못해 구독자에서 뺐습니다", e);
        }
    }
}
