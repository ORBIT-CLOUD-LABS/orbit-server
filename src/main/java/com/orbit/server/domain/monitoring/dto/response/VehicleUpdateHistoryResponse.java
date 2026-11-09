package com.orbit.server.domain.monitoring.dto.response;

import com.orbit.server.domain.monitoring.service.VehicleUpdateAttempt;
import java.util.List;

/**
 * 차량 업데이트 이력 응답.
 *
 * <p>배열을 바로 내려주지 않고 감싼다. 페이지 정보 같은 필드가 필요해져도 응답 계약을 깨지 않고 추가하기 위해서다.
 *
 * @param events 최근 업데이트 시도. 완료 시각이 늦은 순
 */
public record VehicleUpdateHistoryResponse(List<VehicleUpdateEventResponse> events) {

    public static VehicleUpdateHistoryResponse from(List<VehicleUpdateAttempt> attempts) {
        return new VehicleUpdateHistoryResponse(
                attempts.stream().map(VehicleUpdateEventResponse::from).toList());
    }
}
