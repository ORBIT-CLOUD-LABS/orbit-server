package com.orbit.server.domain.vehicle.repository;

import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.service.VehicleProfile;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 등록한 차량 저장소. */
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * 차량을 조회하며 트랜잭션이 끝날 때까지 잠근다. 같은 차량의 재등록이 동시에 들어와도 토큰 폐기·발급을 순서대로 처리하기 위해 쓴다.
     * (VEHICLE-POL-003)
     *
     * <p>트랜잭션 안에서 호출해야 한다. 트랜잭션 밖에서는 조회가 끝나는 즉시 잠금이 풀린다.
     *
     * @param externalId 차량이 보낸 식별자
     * @return 잠근 차량. 처음 등록하는 차량이면 빈 값
     */
    // uk_vehicle_external_id 인덱스로 찾아 그 행만 SELECT ... FOR UPDATE 로 잠근다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.externalId = :externalId")
    Optional<Vehicle> lockByExternalId(@Param("externalId") VehicleExternalId externalId);

    /**
     * 잠그지 않고 차량 정보를 조회한다. 체크인은 차량을 읽기만 하므로 재등록과 서로 기다리지 않게 한다.
     *
     * <p>다른 컨텍스트(체크인·매니페스트)에 넘기는 값이라 엔티티 대신 JPQL 생성자 표현식으로 바로 {@link VehicleProfile} 을 만든다.
     *
     * @param externalId 차량이 보낸 식별자
     * @return 이 식별자의 차량 정보. 등록하지 않은 차량이면 빈 값
     */
    // uk_vehicle_external_id 인덱스로 찾는다
    @Query("""
            select new com.orbit.server.domain.vehicle.service.VehicleProfile(
                v.id, v.spec.model, v.spec.hwVersion, v.spec.region)
            from Vehicle v
            where v.externalId = :externalId
            """)
    Optional<VehicleProfile> findProfileByExternalId(@Param("externalId") VehicleExternalId externalId);
}
