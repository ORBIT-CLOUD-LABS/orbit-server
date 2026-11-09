package com.orbit.server.domain.edge.service;

import java.time.Duration;
import java.util.Objects;

/**
 * Edge 를 고를 때 쓰는 차량의 마지막 위치. 위치가 오래됐으면 쓰지 않으므로 받은 뒤 지난 시간을 함께 받는다.
 *
 * @param latitude 위도
 * @param longitude 경도
 * @param age 위치를 받은 뒤 지난 시간. null 불가
 */
public record ObservedLocation(double latitude, double longitude, Duration age) {

    public ObservedLocation {
        Objects.requireNonNull(age, "age");
    }
}
