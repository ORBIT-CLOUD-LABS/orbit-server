package com.orbit.server.domain.monitoring.service;

import java.time.Instant;

/**
 * [Phase2] 차량이 체크인으로 마지막에 알려 준 GPS 위치. 대시보드 지도에 차량을 그릴 때 쓴다.
 *
 * <p>update 컨텍스트의 위치 값 객체를 참조하지 않고 보여 줄 값만 담는다. 범위 검증은 체크인 때 이미 했다.
 *
 * @param latitude 위도
 * @param longitude 경도
 * @param updatedAt 위치를 받은 시각
 */
public record VehicleLastLocation(double latitude, double longitude, Instant updatedAt) {}
