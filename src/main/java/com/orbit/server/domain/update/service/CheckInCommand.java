package com.orbit.server.domain.update.service;

import java.util.Objects;

/**
 * 체크인 요청.
 *
 * @param vehicleId 요청 경로의 차량 식별자. 차량 토큰 인증을 통과한 값이다
 * @param currentVersion 차량의 현재 소프트웨어 버전. 형식은 차량 상태를 기록할 때 검증한다
 */
public record CheckInCommand(String vehicleId, String currentVersion) {

    public CheckInCommand {
        Objects.requireNonNull(vehicleId, "vehicleId");
    }
}
