package com.orbit.server.domain.monitoring.service;

import java.time.Instant;

// 차량 상태의 필드가 늘 때 값 자체를 검증하지 않는 테스트까지 생성자 호출을 고치지 않도록 한 곳에서 만든다
final class VehicleStatusFixture {

    private static final String MODEL = "model-a";
    private static final String CURRENT_VERSION = "1.0.0";

    private VehicleStatusFixture() {}

    /**
     * @param vehicleId 차량이 보내는 식별자
     * @param updatedAt 상태 변경 시각. 마지막 체크인 시각도 같은 값으로 둔다
     * @return 업데이트 결과가 없는 차량 상태
     */
    static VehicleStatus withoutResult(String vehicleId, Instant updatedAt) {
        return new VehicleStatus(vehicleId, MODEL, CURRENT_VERSION, updatedAt, updatedAt, null, null, null);
    }
}
