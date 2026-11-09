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

    // 지구 평균 반지름(km). 차량과 Edge 의 거리는 가까운 순서를 정하는 데만 쓰므로 구면 근사로 충분하다
    private static final double EARTH_RADIUS_KM = 6371.0088;

    /**
     * @throws InvalidValueException 위도나 경도가 범위를 벗어나거나 NaN 인 경우
     */
    public Coordinates {
        requireWithin(latitude, MAX_LATITUDE, "위도");
        requireWithin(longitude, MAX_LONGITUDE, "경도");
    }

    /**
     * 다른 지점까지의 대권 거리(Haversine)를 km 로 계산한다. 차량과 가까운 Edge 를 고를 때 쓴다.
     *
     * @param other 거리를 잴 지점. null 불가
     * @return 두 지점 사이의 거리(km). 같은 지점이면 0
     */
    public double distanceKmTo(Coordinates other) {
        double latitudeDelta = Math.toRadians(other.latitude - latitude);
        double longitudeDelta = Math.toRadians(other.longitude - longitude);
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(latitude))
                        * Math.cos(Math.toRadians(other.latitude))
                        * Math.pow(Math.sin(longitudeDelta / 2), 2);
        // 정반대에 가까운 두 지점이면 부동소수 오차로 1 을 조금 넘어 asin 이 NaN 이 되므로 1 로 자른다
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(Math.min(1, haversine)));
    }

    // 범위를 벗어난 경우가 아니라 범위 안인 경우를 판정해야 NaN 도 거부된다. NaN 은 어떤 비교에서도 false 이기 때문이다
    private static void requireWithin(double value, double max, String name) {
        if (!(value >= -max && value <= max)) {
            throw new InvalidValueException(name + " 은(는) " + -max + " 이상 " + max + " 이하여야 합니다.");
        }
    }
}
