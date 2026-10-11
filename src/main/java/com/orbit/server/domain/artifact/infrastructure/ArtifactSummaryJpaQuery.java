package com.orbit.server.domain.artifact.infrastructure;

import com.orbit.server.domain.artifact.application.ArtifactSummary;
import com.orbit.server.domain.artifact.application.ArtifactSummaryQuery;
import com.orbit.server.domain.artifact.model.Artifact;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 로 구현한 {@link ArtifactSummaryQuery}.
 *
 * <p>Artifact 에 getter 를 두지 않으므로 엔티티를 읽지 않고 JPQL 생성자 표현식으로 바로 {@link ArtifactSummary} 를 만든다.
 * 같은 밀리초에 등록된 파일도 순서가 정해지도록 ID 를 두 번째 정렬 기준으로 쓴다.
 */
interface ArtifactSummaryJpaQuery extends Repository<Artifact, Long>, ArtifactSummaryQuery {

    @Override
    @Query("""
            select new com.orbit.server.domain.artifact.application.ArtifactSummary(
                a.id, a.release.model, a.release.version, a.file.sizeBytes, a.file.sha256, a.file.path, a.createdAt)
            from Artifact a
            order by a.createdAt desc, a.id desc
            """)
    List<ArtifactSummary> findAll();

    @Override
    @Query("""
            select new com.orbit.server.domain.artifact.application.ArtifactSummary(
                a.id, a.release.model, a.release.version, a.file.sizeBytes, a.file.sha256, a.file.path, a.createdAt)
            from Artifact a
            where a.release.model = :model
            order by a.createdAt desc, a.id desc
            """)
    List<ArtifactSummary> findAllByModel(@Param("model") String model);
}
