package com.orbit.server.domain.vehicle.repository;

import com.orbit.server.domain.vehicle.entity.TokenHash;
import com.orbit.server.domain.vehicle.entity.VehicleCredential;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 차량 토큰 저장소. 토큰은 해시로만 저장한다. */
public interface VehicleCredentialRepository extends JpaRepository<VehicleCredential, Long> {

    /**
     * 유효 토큰의 차량 식별자를 찾는다. 차량 API 요청마다 토큰을 확인할 때 쓴다. (VEHICLE-POL-003)
     *
     * @param tokenHash 차량이 보낸 토큰의 해시
     * @return 토큰을 발급받은 차량의 식별자. 토큰이 없거나 폐기됐으면 빈 값
     */
    default Optional<VehicleExternalId> findActiveVehicleExternalId(TokenHash tokenHash) {
        return findActiveExternalIdByTokenHash(tokenHash.value()).map(VehicleExternalId::new);
    }

    /**
     * {@link #findActiveVehicleExternalId} 가 쓰는 조회. 값 객체로 감싸지 않은 식별자가 필요할 때가 아니면 그쪽을 쓴다.
     *
     * @param tokenHash 토큰 해시 문자열
     * @return 토큰을 발급받은 차량의 식별자 문자열. 토큰이 없거나 폐기됐으면 빈 값
     */
    // 모든 차량 API 요청이 거치므로 유효 토큰 해시 UNIQUE 인덱스가 걸린 생성 컬럼으로 조회한다.
    // 이 컬럼은 ACTIVE 일 때만 값이 있어 상태 조건을 따로 걸지 않아도 폐기한 토큰은 찾지 않는다
    @Query(value = """
                    SELECT v.external_id
                    FROM vehicle_credential c
                    JOIN vehicle v ON v.id = c.vehicle_id
                    WHERE c.active_token_hash = :tokenHash
                    """, nativeQuery = true)
    Optional<String> findActiveExternalIdByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * 차량의 유효 토큰을 폐기한다. 새 토큰을 발급하기 전에 호출해야 차량당 유효 토큰 하나 제약을 어기지 않는다. (VEHICLE-POL-003)
     *
     * @param vehicleId 차량 ID
     * @return 폐기한 토큰 수. 처음 등록하는 차량이면 0
     */
    // 폐기 시각을 발급 시각(issued_at 기본값)과 같은 DB 시계로 기록하도록 NOW(3) 를 쓰는 native UPDATE 로 한 번에 바꾼다
    @Modifying(flushAutomatically = true)
    @Query(value = """
                    UPDATE vehicle_credential
                    SET status = 'REVOKED', revoked_at = NOW(3)
                    WHERE vehicle_id = :vehicleId AND status = 'ACTIVE'
                    """, nativeQuery = true)
    int revokeActiveByVehicleId(@Param("vehicleId") Long vehicleId);
}
