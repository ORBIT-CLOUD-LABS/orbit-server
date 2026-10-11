package com.orbit.server.domain.artifact.repository;

import com.orbit.server.domain.artifact.entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 로 구현한 {@link ArtifactRepository}.
 *
 * <p>service 는 {@link ArtifactRepository} 로만 주입받으므로, {@link JpaRepository} 의 삭제·일괄 저장 같은
 * 메서드는 밖에서 호출할 수 없다.
 */
interface ArtifactJpaRepository extends JpaRepository<Artifact, Long>, ArtifactRepository {}
