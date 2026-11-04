package com.orbit.server.domain.update.service;

import com.orbit.server.domain.update.entity.VehicleLocation;
import java.util.Objects;
import java.util.Optional;

/**
 * 체크인 요청.
 *
 * @param vehicleId 요청 경로의 차량 식별자. 차량 토큰 인증을 통과한 값이다
 * @param currentVersion 차량의 현재 소프트웨어 버전. 형식은 차량 상태를 기록할 때 검증한다
 * @param location 차량의 현재 위치. GPS 를 받지 못해 보내지 않았으면 빈 값
 * @param lastUpdate 지난 체크인 이후 마친 업데이트의 결과. 보고할 결과가 없으면 빈 값
 */
public record CheckInCommand(
        String vehicleId,
        String currentVersion,
        Optional<VehicleLocation> location,
        Optional<ReportUpdateResultCommand> lastUpdate) {

    public CheckInCommand {
        Objects.requireNonNull(vehicleId, "vehicleId");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(lastUpdate, "lastUpdate");
    }

    /**
     * 위치와 보고할 업데이트 결과가 없는 체크인 요청.
     *
     * @param vehicleId 요청 경로의 차량 식별자
     * @param currentVersion 차량의 현재 소프트웨어 버전
     */
    public CheckInCommand(String vehicleId, String currentVersion) {
        this(vehicleId, currentVersion, Optional.empty(), Optional.empty());
    }
}
