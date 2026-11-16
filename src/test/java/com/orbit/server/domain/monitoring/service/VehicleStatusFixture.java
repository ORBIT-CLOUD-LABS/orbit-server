package com.orbit.server.domain.monitoring.service;

import java.time.Instant;

// 차량 상태의 필드가 늘 때 값 자체를 검증하지 않는 테스트까지 생성자 호출을 고치지 않도록 한 곳에서 만든다
final class VehicleStatusFixture {

    private static final String MODEL = "model-a";
    private static final String CURRENT_VERSION = "1.0.0";
    private static final Long FAILED_CAMPAIGN_ID = 1L;
    private static final String FAILED_RESULT = "FAILED";
    private static final String FAILURE_REASON = "HASH_MISMATCH";

    private VehicleStatusFixture() {}

    /**
     * @param vehicleId 차량이 보내는 식별자
     * @param updatedAt 상태 변경 시각. 마지막 체크인 시각도 같은 값으로 둔다
     * @return 업데이트 결과가 없는 차량 상태
     */
    static VehicleStatus withoutResult(String vehicleId, Instant updatedAt) {
        return withVersion(vehicleId, CURRENT_VERSION, updatedAt);
    }

    /**
     * @param vehicleId 차량이 보내는 식별자
     * @param currentVersion 차량의 현재 소프트웨어 버전
     * @param updatedAt 상태 변경 시각. 마지막 체크인 시각도 같은 값으로 둔다
     * @return 업데이트 결과가 없고 버전을 지정한 차량 상태
     */
    static VehicleStatus withVersion(String vehicleId, String currentVersion, Instant updatedAt) {
        return new VehicleStatus(
                vehicleId, MODEL, currentVersion, updatedAt, updatedAt, null, null, null, null, null, null);
    }

    /**
     * @param vehicleId 차량이 보내는 식별자
     * @param updatedAt 상태 변경 시각. 마지막 체크인 시각도 같은 값으로 둔다
     * @return 최근 업데이트가 실패한 차량 상태
     */
    static VehicleStatus withFailedResult(String vehicleId, Instant updatedAt) {
        return new VehicleStatus(
                vehicleId,
                MODEL,
                CURRENT_VERSION,
                updatedAt,
                updatedAt,
                FAILED_CAMPAIGN_ID,
                FAILED_RESULT,
                FAILURE_REASON,
                null,
                null,
                null);
    }
}
