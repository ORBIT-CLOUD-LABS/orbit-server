package com.orbit.server.domain.artifact.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 등록한 Origin 파일 조회.
 *
 * <p>DB 에서 응답 형태로 바로 읽는 조회 전용 경로라 쓰기용 {@link com.orbit.server.domain.artifact.repository.ArtifactRepository} 와
 * 나눈다. 구현은 repository 에 둔다.
 *
 * <p>Artifact 는 차종×버전 수만큼만 쌓이므로 페이지 없이 전체를 돌려준다.
 */
public interface ArtifactSummaryQuery {

    /**
     * @return 모든 Origin 파일. 최근 등록한 순서. 없으면 빈 목록
     */
    List<ArtifactSummary> findAll();

    /**
     * @param model 차종
     * @return 이 차종의 Origin 파일. 최근 등록한 순서. 없으면 빈 목록
     */
    List<ArtifactSummary> findAllByModel(String model);

    /**
     * 캠페인 등록 때 고른 파일의 차종·버전을 확인하기 위해 쓴다.
     *
     * @param id Artifact ID
     * @return 이 ID 의 Origin 파일. 없으면 빈 값
     */
    Optional<ArtifactSummary> findById(Long id);

    /**
     * 캠페인 목록에 목표 버전을 채울 때 캠페인마다 따로 조회하지 않도록 한 번에 읽는다.
     *
     * @param ids Artifact ID. 비어 있으면 빈 목록
     * @return 이 ID 들의 Origin 파일. 순서는 정하지 않으며 없는 ID 는 빠진다
     */
    List<ArtifactSummary> findAllByIds(Collection<Long> ids);
}
