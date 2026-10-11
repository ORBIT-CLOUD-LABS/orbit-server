package com.orbit.server.domain.artifact.infrastructure;

import com.orbit.server.domain.artifact.model.Artifact;
import com.orbit.server.domain.artifact.model.ArtifactRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 로 구현한 {@link ArtifactRepository}.
 *
 * <p>application 은 model 의 {@link ArtifactRepository} 로만 주입받으므로, {@link JpaRepository} 의 삭제·일괄 저장 같은
 * 메서드는 밖에서 호출할 수 없다.
 */
interface ArtifactJpaRepository extends JpaRepository<Artifact, Long>, ArtifactRepository {}
