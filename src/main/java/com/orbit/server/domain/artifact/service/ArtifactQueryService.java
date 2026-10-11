package com.orbit.server.domain.artifact.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 캠페인 등록 때 고를 수 있도록 등록한 Origin 파일 목록을 조회한다. (ORIGIN-REQ-001)
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
}
