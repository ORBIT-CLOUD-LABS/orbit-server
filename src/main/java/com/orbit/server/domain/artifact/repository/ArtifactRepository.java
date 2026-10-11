package com.orbit.server.domain.artifact.repository;

import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;

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
}
