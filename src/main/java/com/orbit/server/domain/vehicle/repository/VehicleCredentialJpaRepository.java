package com.orbit.server.domain.vehicle.repository;

import com.orbit.server.domain.vehicle.entity.VehicleCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 로 구현한 {@link VehicleCredentialRepository}.
 *
 * <p>service 는 {@link VehicleCredentialRepository} 로만 주입받으므로, {@link JpaRepository} 의 삭제·일괄 저장 같은
 * 메서드는 밖에서 호출할 수 없다.
 */
interface VehicleCredentialJpaRepository extends JpaRepository<VehicleCredential, Long>, VehicleCredentialRepository {

    // 폐기 시각을 발급 시각(issued_at 기본값)과 같은 DB 시계로 기록하도록 NOW(3) 를 쓰는 native UPDATE 로 한 번에 바꾼다
    @Override
    @Modifying(flushAutomatically = true)
    @Query(value = """
                    UPDATE vehicle_credential
                    SET status = 'REVOKED', revoked_at = NOW(3)
                    WHERE vehicle_id = :vehicleId AND status = 'ACTIVE'
                    """, nativeQuery = true)
    int revokeActiveByVehicleId(@Param("vehicleId") Long vehicleId);
}
