package com.orbit.server.domain.campaign.entity;

import java.util.Objects;

/**
 * 캠페인 대상인지 판정할 차량. 등록 때 받은 차종·HW 버전·지역과 체크인 때 보낸 현재 버전을 함께 본다. (VEHICLE-POL-001)
 *
 * <p>차량 컨텍스트의 타입에 의존하지 않도록 판정에 필요한 값만 따로 받는다.
 *
 * @param model 차종. null 불가
 * @param hwVersion HW 버전. null 불가
 * @param region 지역. null 불가
 * @param currentVersion 현재 소프트웨어 버전. null 불가
 */
public record TargetVehicle(String model, String hwVersion, String region, String currentVersion) {

    public TargetVehicle {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(hwVersion, "hwVersion");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(currentVersion, "currentVersion");
    }
}
