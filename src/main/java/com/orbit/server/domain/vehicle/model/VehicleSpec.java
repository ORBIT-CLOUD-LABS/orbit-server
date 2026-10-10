package com.orbit.server.domain.vehicle.model;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 캠페인 대상 판정에 쓰는 차량의 차종·HW 버전·지역.
 *
 * @param model 차종. 공백 불가, 50자 이하
 * @param hwVersion HW 버전. 공백 불가, 50자 이하
 * @param region 지역. 공백 불가, 50자 이하
 */
@Embeddable
public record VehicleSpec(
        @Column(name = "model") String model,
        @Column(name = "hw_version") String hwVersion,
        @Column(name = "region") String region) {

    private static final int MAX_LENGTH = 50;

    /**
     * @throws InvalidValueException 값이 비어 있거나 50자를 넘는 경우
     */
    public VehicleSpec {
        Preconditions.requireText(model, MAX_LENGTH, "차종");
        Preconditions.requireText(hwVersion, MAX_LENGTH, "HW 버전");
        Preconditions.requireText(region, MAX_LENGTH, "지역");
    }
}
