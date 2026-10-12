package com.orbit.server.domain.vehicle.repository;

import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 로 구현한 {@link VehicleRepository}.
 *
 * <p>service 는 {@link VehicleRepository} 로만 주입받으므로, {@link JpaRepository} 의 삭제·일괄 저장 같은
 * 메서드는 밖에서 호출할 수 없다.
 */
interface VehicleJpaRepository extends JpaRepository<Vehicle, Long>, VehicleRepository {

    // uk_vehicle_external_id 인덱스로 찾아 그 행만 SELECT ... FOR UPDATE 로 잠근다
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.externalId = :externalId")
    Optional<Vehicle> lockByExternalId(@Param("externalId") VehicleExternalId externalId);
}
