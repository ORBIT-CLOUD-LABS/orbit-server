package com.orbit.server.domain.update.dto.request;

import com.orbit.server.domain.update.service.CheckInCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 체크인 요청. 업데이트 결과({@code lastUpdate})는 별도 작업에서 받는다.
 *
 * @param currentVersion 현재 소프트웨어 버전
 */
public record CheckInRequest(@NotBlank String currentVersion) {

    /**
     * @param vehicleId 요청 경로의 차량 식별자
     * @return 체크인 유스케이스 요청
     */
    public CheckInCommand toCommand(String vehicleId) {
        return new CheckInCommand(vehicleId, currentVersion);
    }
}
