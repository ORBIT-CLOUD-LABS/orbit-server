package com.orbit.server.domain.update.repository;

import com.orbit.server.domain.update.entity.VehicleState;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 로 구현한 {@link VehicleStateRepository}.
 *
 * <p>service 는 {@link VehicleStateRepository} 로만 주입받으므로, {@link JpaRepository} 의 삭제·일괄 저장 같은
 * 메서드는 밖에서 호출할 수 없다.
 */
interface VehicleStateJpaRepository extends JpaRepository<VehicleState, Long>, VehicleStateRepository {}
