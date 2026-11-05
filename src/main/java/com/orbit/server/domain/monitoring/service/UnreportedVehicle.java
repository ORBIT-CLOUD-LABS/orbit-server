package com.orbit.server.domain.monitoring.service;

import java.util.Objects;

/**
 * 한 캠페인에 업데이트 결과를 아직 보고하지 않은 차량. 캠페인 대상 조건으로 진행 중인지 판정하는 데 쓴다.
 *
 * @param model 차종. null 불가
 * @param hwVersion HW 버전. null 불가
 * @param region 지역. null 불가
 * @param currentVersion 마지막 체크인 때 기록한 현재 소프트웨어 버전. null 불가
 */
public record UnreportedVehicle(String model, String hwVersion, String region, String currentVersion) {

    public UnreportedVehicle {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(hwVersion, "hwVersion");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(currentVersion, "currentVersion");
    }
}
