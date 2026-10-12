package com.orbit.server.domain.vehicle.repository;

import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.service.VehicleProfile;
import com.orbit.server.domain.vehicle.service.VehicleProfileQuery;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 로 구현한 {@link VehicleProfileQuery}.
 *
 * <p>Vehicle 에 getter 를 두지 않으므로 엔티티를 읽지 않고 JPQL 생성자 표현식으로 바로 {@link VehicleProfile} 을 만든다.
 */
interface VehicleProfileJpaQuery extends Repository<Vehicle, Long>, VehicleProfileQuery {

    // uk_vehicle_external_id 인덱스로 찾는다
    @Override
    @Query("""
            select new com.orbit.server.domain.vehicle.service.VehicleProfile(
                v.id, v.spec.model, v.spec.hwVersion, v.spec.region)
            from Vehicle v
            where v.externalId = :externalId
            """)
    Optional<VehicleProfile> findByExternalId(@Param("externalId") VehicleExternalId externalId);
}
