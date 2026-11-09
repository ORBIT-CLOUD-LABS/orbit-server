package com.orbit.server.domain.update.service;

import com.orbit.server.domain.update.entity.SoftwareVersion;
import com.orbit.server.domain.update.entity.VehicleLocation;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 매니페스트 발급에 쓰는 차량 상태. 대상을 다시 판정할 현재 버전과 Edge 를 고를 마지막 위치다.
 *
 * @param vehicleId 차량 ID
 * @param currentVersion 마지막 체크인 때 기록한 현재 버전
 * @param lastLocation 마지막 위치와 받은 뒤 지난 시간. 위치를 받은 적 없으면 빈 값
 */
public record ManifestVehicleState(
        Long vehicleId, SoftwareVersion currentVersion, Optional<LastLocation> lastLocation) {

    public ManifestVehicleState {
        Objects.requireNonNull(vehicleId, "vehicleId");
        Objects.requireNonNull(currentVersion, "currentVersion");
        Objects.requireNonNull(lastLocation, "lastLocation");
    }

    /**
     * 차량이 마지막으로 보낸 위치.
     *
     * @param location 위치
     * @param age 위치를 받은 뒤 지난 시간. DB 시계로 잰다
     */
    public record LastLocation(VehicleLocation location, Duration age) {

        public LastLocation {
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(age, "age");
        }
    }
}
