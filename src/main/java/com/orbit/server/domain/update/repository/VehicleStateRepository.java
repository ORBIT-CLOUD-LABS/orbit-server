package com.orbit.server.domain.update.repository;

import com.orbit.server.domain.update.entity.SoftwareVersion;
import com.orbit.server.domain.update.entity.VehicleLocation;
import com.orbit.server.domain.update.entity.VehicleState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 차량 현재 상태 저장소. 차량 하나에 상태 하나라 Vehicle ID 가 그대로 ID 다. */
public interface VehicleStateRepository extends JpaRepository<VehicleState, Long> {

    /**
     * 차량의 현재 버전만 조회한다. 매니페스트 발급에서 마지막 체크인 때 기록한 버전으로 대상을 다시 판정할 때 쓴다.
     * (MANIFEST-POL-001)
     *
     * @param vehicleId 차량 ID
     * @return 차량의 현재 버전. 상태가 없으면 빈 값
     */
    // 판정에는 버전만 필요하므로 엔티티를 읽지 않고 버전 값만 조회한다
    @Query("select s.currentVersion from VehicleState s where s.vehicleId = :vehicleId")
    Optional<SoftwareVersion> findCurrentVersionById(@Param("vehicleId") Long vehicleId);

    /**
     * 체크인한 차량의 현재 버전과 마지막 체크인 시각을 덮어쓴다. 마지막 체크인 시각은 DB 시계로 기록한다. (VEHICLE-REQ-001)
     *
     * @param vehicleId 차량 ID
     * @param currentVersion 차량이 보낸 현재 버전
     * @return 덮어쓴 행 수. 상태가 없는 차량이면 0
     */
    default int recordCheckIn(Long vehicleId, SoftwareVersion currentVersion) {
        return updateCheckIn(vehicleId, currentVersion.value());
    }

    /**
     * {@link #recordCheckIn} 이 쓰는 갱신 쿼리. 값 객체를 컬럼 값으로 풀어서 받으므로 직접 쓰지 않고 {@link #recordCheckIn} 을 쓴다.
     */
    // 여러 pod 가 기록해도 마지막 체크인 시각이 같은 시계를 따르도록 NOW(3) 를 쓰는 native UPDATE 로 한 번에 바꾼다.
    // 조회 없이 덮어쓰므로 같은 차량의 체크인이 동시에 와도 행 잠금으로 순서대로 반영된다.
    // 반환값은 조건에 맞은 행 수다. Connector/J 기본값(useAffectedRows=false)에 기대므로 같은 값으로 덮어써도 1 이다.
    // JDBC URL 에 useAffectedRows=true 를 넣으면 값이 그대로인 체크인이 0 이 되어 상태 없음으로 오판한다
    @Modifying(flushAutomatically = true)
    @Query(value = """
                    UPDATE vehicle_state
                    SET current_version = :currentVersion, last_seen_at = NOW(3)
                    WHERE vehicle_id = :vehicleId
                    """, nativeQuery = true)
    int updateCheckIn(@Param("vehicleId") Long vehicleId, @Param("currentVersion") String currentVersion);

    /**
     * 차량이 체크인으로 보낸 위치와 위치 갱신 시각을 덮어쓴다. 갱신 시각은 마지막 체크인 시각처럼 DB 시계로 기록한다.
     * (VEHICLE-REQ-001)
     *
     * @param vehicleId 차량 ID
     * @param location 차량이 보낸 위치
     * @return 덮어쓴 행 수. 상태가 없는 차량이면 0
     */
    default int recordLocation(Long vehicleId, VehicleLocation location) {
        return updateLocation(vehicleId, location.latitude(), location.longitude());
    }

    /**
     * {@link #recordLocation} 이 쓰는 갱신 쿼리. 값 객체를 컬럼 값으로 풀어서 받으므로 직접 쓰지 않고 {@link #recordLocation} 을
     * 쓴다.
     */
    // 위치의 신선도(location-max-age)를 마지막 체크인 시각과 같은 시계로 판단하도록 NOW(3) 를 쓴다.
    // 위도·경도·갱신 시각은 함께 있거나 함께 없어야 하므로(ck_vehicle_state_location) 한 문장으로 바꾼다
    @Modifying(flushAutomatically = true)
    @Query(value = """
                    UPDATE vehicle_state
                    SET latitude = :latitude, longitude = :longitude, location_updated_at = NOW(3)
                    WHERE vehicle_id = :vehicleId
                    """, nativeQuery = true)
    int updateLocation(
            @Param("vehicleId") Long vehicleId,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude);
}
