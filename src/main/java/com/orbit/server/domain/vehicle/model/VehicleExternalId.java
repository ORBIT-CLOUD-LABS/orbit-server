package com.orbit.server.domain.vehicle.model;

import com.orbit.server.global.common.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 차량이 등록·체크인 때 보내는 식별자(API 의 {@code vehicleId}). 내부 PK 와 구분하기 위해 따로 둔다.
 *
 * @param value 식별자. 공백 불가, 64자 이하
 */
@Embeddable
public record VehicleExternalId(
        @Column(name = "external_id") String value) {

    private static final int MAX_LENGTH = 64;

    /**
     * @throws IllegalArgumentException 식별자가 비어 있거나 64자를 넘는 경우
     */
    public VehicleExternalId {
        Preconditions.requireText(value, MAX_LENGTH, "차량 식별자");
    }
}
