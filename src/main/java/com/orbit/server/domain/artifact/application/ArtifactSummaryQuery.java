package com.orbit.server.domain.artifact.application;

import java.util.List;

/**
 * 등록한 Origin 파일 목록 조회.
 *
 * <p>DB 에서 응답 형태로 바로 읽는 조회 전용 경로라 쓰기용 {@link com.orbit.server.domain.artifact.model.ArtifactRepository} 와
 * 나눈다. 구현은 infrastructure 에 둔다.
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
}
