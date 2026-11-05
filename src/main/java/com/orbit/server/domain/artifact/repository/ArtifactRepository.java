package com.orbit.server.domain.artifact.repository;

import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.artifact.service.ArtifactSummary;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 등록한 업데이트 파일 저장소.
 *
 * <p>목록·다른 컨텍스트에 넘기는 조회는 엔티티를 읽지 않고 JPQL 생성자 표현식으로 바로 {@link ArtifactSummary} 를 만든다. 같은
 * 밀리초에 등록된 파일도 순서가 정해지도록 ID 를 두 번째 정렬 기준으로 쓴다. Artifact 는 차종×버전 수만큼만 쌓이므로 페이지 없이
 * 전체를 돌려준다.
 */
public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    /**
     * 같은 차종·버전의 파일이 이미 등록되어 있는지 확인한다. 등록 전에 Origin 파일을 덮어쓰지 않도록 업로드보다 먼저 쓴다.
     *
     * @param release 확인할 차종과 버전
     * @return 이미 있으면 true
     */
    boolean existsByRelease(ArtifactRelease release);

    /**
     * 파일 행을 트랜잭션이 끝날 때까지 잠근다. 같은 파일로 캠페인을 동시에 등록하는 요청을 순서대로 처리하기 위해 쓴다.
     * (CAMPAIGN-POL-001)
     *
     * <p>트랜잭션 안에서 호출해야 한다. 트랜잭션 밖에서는 조회가 끝나는 즉시 잠금이 풀린다.
     *
     * @param id 잠글 Artifact ID
     * @return 잠근 Artifact 의 ID. 없으면 빈 값
     */
    // 엔티티 대신 ID 만 읽으며 잠근다. 잠금 SQL 이 확실히 나가도록 native 로 쓴다
    @Query(value = "SELECT id FROM artifact WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockById(@Param("id") Long id);

    /**
     * @return 모든 Origin 파일. 최근 등록한 순서. 없으면 빈 목록
     */
    @Query("""
            select new com.orbit.server.domain.artifact.service.ArtifactSummary(
                a.id, a.release.model, a.release.version, a.file.sizeBytes, a.file.sha256, a.file.path, a.createdAt)
            from Artifact a
            order by a.createdAt desc, a.id desc
            """)
    List<ArtifactSummary> findAllSummaries();

    /**
     * @param model 차종
     * @return 이 차종의 Origin 파일. 최근 등록한 순서. 없으면 빈 목록
     */
    @Query("""
            select new com.orbit.server.domain.artifact.service.ArtifactSummary(
                a.id, a.release.model, a.release.version, a.file.sizeBytes, a.file.sha256, a.file.path, a.createdAt)
            from Artifact a
            where a.release.model = :model
            order by a.createdAt desc, a.id desc
            """)
    List<ArtifactSummary> findSummariesByModel(@Param("model") String model);

    /**
     * 캠페인 등록 때 고른 파일의 차종·버전을 확인하기 위해 쓴다.
     *
     * @param id Artifact ID
     * @return 이 ID 의 Origin 파일. 없으면 빈 값
     */
    @Query("""
            select new com.orbit.server.domain.artifact.service.ArtifactSummary(
                a.id, a.release.model, a.release.version, a.file.sizeBytes, a.file.sha256, a.file.path, a.createdAt)
            from Artifact a
            where a.id = :id
            """)
    Optional<ArtifactSummary> findSummaryById(@Param("id") Long id);

    /**
     * 캠페인 목록에 목표 버전을 채울 때 캠페인마다 따로 조회하지 않도록 한 번에 읽는다.
     *
     * @param ids Artifact ID. 비어 있으면 빈 목록
     * @return 이 ID 들의 Origin 파일. 순서는 정하지 않으며 없는 ID 는 빠진다
     */
    @Query("""
            select new com.orbit.server.domain.artifact.service.ArtifactSummary(
                a.id, a.release.model, a.release.version, a.file.sizeBytes, a.file.sha256, a.file.path, a.createdAt)
            from Artifact a
            where a.id in :ids
            """)
    List<ArtifactSummary> findSummariesByIds(@Param("ids") Collection<Long> ids);
}
