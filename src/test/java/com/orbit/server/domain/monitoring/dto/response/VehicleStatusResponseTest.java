package com.orbit.server.domain.monitoring.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.monitoring.service.result.VehicleLastLocation;
import com.orbit.server.domain.monitoring.service.result.VehicleStatus;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

// SSE 는 응답 DTO 를 거치지 않고 VehicleStatus 를 그대로 내보낸다. 한쪽 record 만 바뀌어 스냅샷과 스트림의 형식이 어긋나면
// 여기서 실패하도록 두 JSON 을 비교한다
class VehicleStatusResponseTest {

    private static final Instant LAST_SEEN_AT = Instant.parse("2026-10-01T12:00:00.123Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T12:00:01.456Z");
    private static final Instant LOCATED_AT = Instant.parse("2026-10-01T12:00:00.789Z");
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Test
    @DisplayName("MONITORING-REQ-001: 모든 값이 있는 차량은 스냅샷 응답과 SSE 가 내보내는 차량 상태의 JSON 이 같다")
    void serializesSameJsonAsStreamedStatusWithAllFields() {
        VehicleStatus status = new VehicleStatus(
                "vehicle-001",
                "model-a",
                "1.0.0",
                LAST_SEEN_AT,
                UPDATED_AT,
                7L,
                "FAILED",
                "SIGNATURE_INVALID",
                new VehicleLastLocation(37.5665, 126.978, LOCATED_AT),
                "edge-seoul",
                "edge-daejeon");

        assertThat(JSON_MAPPER.writeValueAsString(VehicleStatusResponse.from(status)))
                .isEqualTo(JSON_MAPPER.writeValueAsString(status));
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 업데이트 결과·위치·Edge 가 없는 차량도 스냅샷 응답과 SSE 가 내보내는 차량 상태의 JSON 이 같다")
    void serializesSameJsonAsStreamedStatusWithoutOptionalFields() {
        VehicleStatus status = new VehicleStatus(
                "vehicle-001", "model-a", "1.0.0", null, UPDATED_AT, null, null, null, null, null, null);

        assertThat(JSON_MAPPER.writeValueAsString(VehicleStatusResponse.from(status)))
                .isEqualTo(JSON_MAPPER.writeValueAsString(status));
    }
}
