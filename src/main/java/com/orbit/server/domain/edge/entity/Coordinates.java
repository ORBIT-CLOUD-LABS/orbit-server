package com.orbit.server.domain.edge.entity;

import com.orbit.server.global.error.InvalidValueException;

/**
 * 지구 위의 한 점. Edge 의 대표 위치로 쓰고, 차량과 가까운 Edge 를 고를 때 거리 계산의 기준이 된다.
 *
 * @param latitude 위도. -90 이상 90 이하
 * @param longitude 경도. -180 이상 180 이하
 */
public record Coordinates(double latitude, double longitude) {

    private static final double MAX_LATITUDE = 90;
    private static final double MAX_LONGITUDE = 180;

    /**
     * @throws InvalidValueException 위도나 경도가 범위를 벗어나거나 NaN 인 경우
     */
    public Coordinates {
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
