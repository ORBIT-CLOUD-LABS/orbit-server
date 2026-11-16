package com.orbit.server.domain.monitoring.service;

import java.time.Instant;
import java.util.Optional;

/**
 * [Phase2] 차량이 체크인으로 마지막에 알려 준 GPS 위치. 대시보드 지도에 차량을 그릴 때 쓴다.
 *
 * <p>update 컨텍스트의 위치 값 객체를 참조하지 않고 보여 줄 값만 담는다. 범위 검증은 체크인 때 이미 했다.
 *
 * @param latitude 위도
 * @param longitude 경도
 * @param updatedAt 위치를 받은 시각
 */
public record VehicleLastLocation(double latitude, double longitude, Instant updatedAt) {

    /**
     * vehicle_status 뷰의 위치 컬럼으로 마지막 위치를 만든다. 위치를 받은 적 없으면 빈 값이다.
     *
     * @param latitude 위도 컬럼. 위치를 받은 적 없으면 null
     * @param longitude 경도 컬럼. 위치를 받은 적 없으면 null
     * @param updatedAt 위치를 받은 시각 컬럼. 위치를 받은 적 없으면 null
     * @return 세 값이 모두 있으면 마지막 위치, 하나라도 없으면 빈 값
     */
    // 세 컬럼은 DB 제약(ck_vehicle_state_location)으로 함께 있거나 함께 없다. 그래도 제약이 어긋난 행 하나 때문에
    // 차량 전체를 읽는 스냅샷·SSE 조회가 실패하지 않도록, 하나라도 없으면 예외 대신 위치가 없는 것으로 본다
    public static Optional<VehicleLastLocation> of(Double latitude, Double longitude, Instant updatedAt) {
        if (latitude == null || longitude == null || updatedAt == null) {
            return Optional.empty();
        }
        return Optional.of(new VehicleLastLocation(latitude, longitude, updatedAt));
    }
}
