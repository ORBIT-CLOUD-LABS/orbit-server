package com.orbit.server.domain.update.repository;

import java.math.BigDecimal;

/**
 * 매니페스트 발급에 필요한 차량 상태 한 행. {@link VehicleStateRepository#findManifestStateById} 의 조회 결과다.
 *
 * <p>위치를 받은 적 없으면 위도·경도·위치 경과 시간이 모두 null 이다.
 */
public interface ManifestStateRow {

    /** 마지막 체크인 때 기록한 현재 버전. */
    String getCurrentVersion();

    /** 마지막 위치의 위도. 위치가 없으면 null. */
    BigDecimal getLatitude();

    /** 마지막 위치의 경도. 위치가 없으면 null. */
    BigDecimal getLongitude();

    /** DB 시계로 잰, 위치를 받은 뒤 지난 시간(마이크로초). 위치가 없으면 null. */
    Long getLocationAgeMicros();
}
