package com.orbit.server.domain.update.entity;

import com.orbit.server.global.error.InvalidValueException;

/**
 * 차량이 체크인으로 알려 준 마지막 GPS 위치. 매니페스트가 차량과 가까운 Edge 를 고를 때 쓰고, 대상 판정에는 쓰지 않는다.
 *
 * <p>Edge 좌표({@code edge} 컨텍스트의 {@code Coordinates})와 같은 범위 규칙이지만, 다른 컨텍스트의 entity 를 참조하지 않도록 이 컨텍스트에
 * 따로 둔다. DB 검사 제약(ck_vehicle_state_latitude·longitude)과 같은 규칙이다.
 *
 * @param latitude 위도. -90 이상 90 이하
 * @param longitude 경도. -180 이상 180 이하
 */
public record VehicleLocation(double latitude, double longitude) {

    private static final double MAX_LATITUDE = 90;
    private static final double MAX_LONGITUDE = 180;

    /**
     * @throws InvalidValueException 위도나 경도가 범위를 벗어나거나 NaN 인 경우
     */
    public VehicleLocation {
        requireWithin(latitude, MAX_LATITUDE, "위도");
        requireWithin(longitude, MAX_LONGITUDE, "경도");
    }

    // 범위를 벗어난 경우가 아니라 범위 안인 경우를 판정해야 NaN 도 거부된다. NaN 은 어떤 비교에서도 false 이기 때문이다
    private static void requireWithin(double value, double max, String name) {
        if (!(value >= -max && value <= max)) {
            throw new InvalidValueException(name + " 은(는) " + -max + " 이상 " + max + " 이하여야 합니다.");
        }
    }
}
