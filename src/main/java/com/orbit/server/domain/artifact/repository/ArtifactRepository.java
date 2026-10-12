package com.orbit.server.domain.artifact.repository;

import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import java.util.Optional;

/** 등록한 업데이트 파일 저장소. */
public interface ArtifactRepository {

    /**
     * 같은 차종·버전의 파일이 이미 등록되어 있는지 확인한다. 등록 전에 Origin 파일을 덮어쓰지 않도록 업로드보다 먼저 쓴다.
     *
     * @param release 확인할 차종과 버전
     * @return 이미 있으면 true
     */
    boolean existsByRelease(ArtifactRelease release);

    /**
     * 업데이트 파일을 저장한다.
     *
     * @param artifact 저장할 파일
     * @return ID 가 채워진 파일
     */
    Artifact save(Artifact artifact);

    /**
     * 파일 행을 트랜잭션이 끝날 때까지 잠근다. 같은 파일로 캠페인을 동시에 등록하는 요청을 순서대로 처리하기 위해 쓴다.
     * (CAMPAIGN-POL-001)
     *
     * <p>트랜잭션 안에서 호출해야 한다. 트랜잭션 밖에서는 조회가 끝나는 즉시 잠금이 풀린다.
     *
     * @param id 잠글 Artifact ID
     * @return 잠근 Artifact 의 ID. 없으면 빈 값
     */
    Optional<Long> lockById(Long id);
}
