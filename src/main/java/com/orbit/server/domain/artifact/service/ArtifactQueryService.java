package com.orbit.server.domain.artifact.service;

import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.global.error.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 캠페인 등록 때 고를 수 있도록 등록한 Origin 파일을 조회한다. (ORIGIN-REQ-001, CAMPAIGN-REQ-001)
 *
 * <p>조회 한 번뿐이라 트랜잭션으로 묶지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ArtifactQueryService {

    private final ArtifactSummaryQuery artifactSummaryQuery;

    /**
     * 차종이 있으면 그 차종의 파일만, 없으면 전체 파일을 조회한다.
     *
     * @param model 차종. 선택이며, null 이거나 공백이면 필터링하지 않는다
     * @return 최근 등록한 순서의 Origin 파일. 없으면 빈 목록
     */
    public List<ArtifactSummary> findAll(String model) {
        // ?model= 처럼 값 없이 온 필터는 필터가 없는 것으로 본다
        if (!StringUtils.hasText(model)) {
            return artifactSummaryQuery.findAll();
        }
        return artifactSummaryQuery.findAllByModel(model);
    }

    /**
     * 캠페인 등록 때 고른 파일을 조회한다. 캠페인 컨텍스트는 Artifact 를 직접 읽지 않고 이 메서드로 차종·버전을 확인한다.
     * (CAMPAIGN-REQ-001)
     *
     * @param artifactId Artifact ID
     * @return 이 ID 의 Origin 파일
     * @throws BusinessException 파일이 없으면 ARTIFACT_NOT_FOUND
     */
    public ArtifactSummary getById(Long artifactId) {
        return artifactSummaryQuery
                .findById(artifactId)
                .orElseThrow(() -> new BusinessException(ArtifactErrorCode.ARTIFACT_NOT_FOUND));
    }
}
