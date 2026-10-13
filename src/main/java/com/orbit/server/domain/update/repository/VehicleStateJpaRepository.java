package com.orbit.server.domain.update.repository;

import com.orbit.server.domain.update.entity.SoftwareVersion;
import com.orbit.server.domain.update.entity.VehicleState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 로 구현한 {@link VehicleStateRepository}.
 *
 * <p>service 는 {@link VehicleStateRepository} 로만 주입받으므로, {@link JpaRepository} 의 삭제·일괄 저장 같은
 * 메서드는 밖에서 호출할 수 없다.
 */
interface VehicleStateJpaRepository extends JpaRepository<VehicleState, Long>, VehicleStateRepository {

    // 판정에는 버전만 필요하므로 엔티티를 읽지 않고 버전 값만 조회한다
    @Override
    @Query("select s.currentVersion from VehicleState s where s.vehicleId = :vehicleId")
    Optional<SoftwareVersion> findCurrentVersionById(@Param("vehicleId") Long vehicleId);

    @Override
    default int recordCheckIn(Long vehicleId, SoftwareVersion currentVersion) {
        return updateCheckIn(vehicleId, currentVersion.value());
    }

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
}
