package com.orbit.server.domain.artifact.dto.response;

import com.orbit.server.domain.artifact.service.ArtifactSummary;
import java.util.List;

/**
 * Origin 파일 목록 응답.
 *
 * <p>배열을 바로 내려주지 않고 감싼다. 페이지 정보 같은 필드가 필요해져도 응답 계약을 깨지 않고 추가하기 위해서다.
 *
 * @param artifacts 최근 등록한 순서의 Origin 파일
 */
public record ArtifactListResponse(List<ArtifactSummaryResponse> artifacts) {

    public static ArtifactListResponse from(List<ArtifactSummary> summaries) {
        return new ArtifactListResponse(
                summaries.stream().map(ArtifactSummaryResponse::from).toList());
    }
}
