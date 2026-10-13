package com.orbit.server.domain.update.service;

import com.orbit.server.domain.artifact.service.ArtifactSummary;

/**
 * 차량이 업데이트 파일을 받고 검증하는 데 필요한 매니페스트. (MANIFEST-REQ-001)
 *
 * @param campaignId 캠페인 ID
 * @param targetVersion 설치할 버전
 * @param fileSize 다운로드한 파일을 검증할 크기(byte)
 * @param sha256 다운로드한 파일을 검증할 SHA-256
 * @param downloadUrl 파일을 받을 CDN URL
 */
public record ManifestResult(Long campaignId, String targetVersion, long fileSize, String sha256, String downloadUrl) {

    /**
     * @param campaignId 캠페인 ID
     * @param artifact 캠페인이 배포하는 파일
     * @param downloadUrl 파일을 받을 CDN URL
     * @return 파일의 버전·크기·해시를 담은 매니페스트
     */
    public static ManifestResult of(Long campaignId, ArtifactSummary artifact, String downloadUrl) {
        return new ManifestResult(campaignId, artifact.version(), artifact.fileSize(), artifact.sha256(), downloadUrl);
    }
}
