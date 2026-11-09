package com.orbit.server.domain.update.service;

import com.orbit.server.domain.artifact.service.ArtifactSummary;
import com.orbit.server.domain.edge.service.SelectedEdge;
import com.orbit.server.domain.update.service.EdgeAssignment.FallbackUrl;
import java.util.List;

/**
 * 차량이 업데이트 파일을 받고 검증하는 데 필요한 매니페스트.
 *
 * @param campaignId 캠페인 ID
 * @param targetVersion 설치할 버전
 * @param fileSize 다운로드한 파일을 검증할 크기(byte)
 * @param sha256 다운로드한 파일을 검증할 SHA-256
 * @param edge 다운로드 URL 을 만든 Edge. 거리는 차량 위치를 모르면 빈 값
 * @param downloadUrl 파일을 먼저 받을 Edge 의 URL
 * @param fallbackUrls 다운로드가 실패하면 순서대로 시도할 예비 Edge 의 URL. 없으면 빈 목록
 */
public record ManifestResult(
        Long campaignId,
        String targetVersion,
        long fileSize,
        String sha256,
        SelectedEdge edge,
        String downloadUrl,
        List<FallbackUrl> fallbackUrls) {

    /**
     * @param campaignId 캠페인 ID
     * @param artifact 캠페인이 배포하는 파일
     * @param assignment 차량에 배정한 Edge 와 다운로드 URL
     * @return 파일의 버전·크기·해시와 Edge 별 다운로드 URL 을 담은 매니페스트
     */
    public static ManifestResult of(Long campaignId, ArtifactSummary artifact, EdgeAssignment assignment) {
        return new ManifestResult(
                campaignId,
                artifact.version(),
                artifact.fileSize(),
                artifact.sha256(),
                assignment.edge(),
                assignment.downloadUrl(),
                assignment.fallbackUrls());
    }
}
