package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.campaign.entity.TargetVehicle;
import java.util.Objects;

/**
 * 캠페인 대상 조회에 쓰는 차량 조건. 다른 컨텍스트(체크인, 매니페스트 발급)가 캠페인 entity 인 {@link TargetVehicle} 을 직접 만들지 않도록
 * 서비스 경계에서 받는다.
 *
 * @param model 차종. null 불가
 * @param hwVersion HW 버전. null 불가
 * @param region 지역. null 불가
 * @param currentVersion 현재 소프트웨어 버전. null 불가
 */
public record TargetVehicleCriteria(String model, String hwVersion, String region, String currentVersion) {

    public TargetVehicleCriteria {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(hwVersion, "hwVersion");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(currentVersion, "currentVersion");
    }

    TargetVehicle toTargetVehicle() {
        return new TargetVehicle(model, hwVersion, region, currentVersion);
    }
}
