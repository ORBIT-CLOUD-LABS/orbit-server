package com.orbit.server.domain.artifact.repository;

import com.orbit.server.domain.artifact.entity.Artifact;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 로 구현한 {@link ArtifactRepository}.
 *
 * <p>service 는 {@link ArtifactRepository} 로만 주입받으므로, {@link JpaRepository} 의 삭제·일괄 저장 같은
 * 메서드는 밖에서 호출할 수 없다.
 */
interface ArtifactJpaRepository extends JpaRepository<Artifact, Long>, ArtifactRepository {

    // Artifact 에 getter 를 두지 않으므로 엔티티 대신 ID 만 읽으며 잠근다. 잠금 SQL 이 확실히 나가도록 native 로 쓴다
    @Override
    @Query(value = "SELECT id FROM artifact WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockById(@Param("id") Long id);
}
