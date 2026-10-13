package com.orbit.server.domain.update.dto.request;

import com.orbit.server.domain.update.service.CheckInCommand;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;

/**
 * 체크인 요청.
 *
 * @param currentVersion 현재 소프트웨어 버전
 * @param lastUpdate 지난 체크인 이후 마친 업데이트의 결과. 보고할 결과가 없으면 null. 형식이 틀려도 체크인은 처리하도록
 *     {@code @Valid} 를 걸지 않는다
 */
public record CheckInRequest(@NotBlank String currentVersion, LastUpdateRequest lastUpdate) {

    /**
     * @param vehicleId 요청 경로의 차량 식별자
     * @return 체크인 유스케이스 요청
     */
    public CheckInCommand toCommand(String vehicleId) {
        return new CheckInCommand(
                vehicleId, currentVersion, Optional.ofNullable(lastUpdate).map(LastUpdateRequest::toCommand));
    }
}
