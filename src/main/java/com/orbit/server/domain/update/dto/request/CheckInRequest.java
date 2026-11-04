package com.orbit.server.domain.update.dto.request;

import com.orbit.server.domain.update.service.CheckInCommand;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;

/**
 * 체크인 요청.
 *
 * @param currentVersion 현재 소프트웨어 버전
 * @param location 차량의 현재 위치. GPS 를 받지 못했으면 보내지 않는다. 보내지 않으면 서버는 이전 위치를 그대로 둔다. 보냈는데 좌표가
 *     없거나 범위를 벗어나면 400 으로 거부한다
 * @param lastUpdate 지난 체크인 이후 마친 업데이트의 결과. 보고할 결과가 없으면 null. 형식이 틀려도 체크인은 처리하도록
 *     {@code @Valid} 를 걸지 않는다
 */
public record CheckInRequest(
        @NotBlank String currentVersion, @Valid LocationRequest location, LastUpdateRequest lastUpdate) {

    /**
     * @param vehicleId 요청 경로의 차량 식별자
     * @return 체크인 유스케이스 요청
     * @throws InvalidValueException 위치의 위도나 경도가 범위를 벗어난 경우
     */
    public CheckInCommand toCommand(String vehicleId) {
        return new CheckInCommand(
                vehicleId,
                currentVersion,
                Optional.ofNullable(location).map(LocationRequest::toLocation),
                Optional.ofNullable(lastUpdate).map(LastUpdateRequest::toCommand));
    }
}
