package com.orbit.server.domain.update.dto.response;

import com.orbit.server.domain.update.service.ManifestResult;

/**
 * 매니페스트 응답. 차량은 {@code downloadUrl} 로 파일을 받고 크기와 해시로 검증한다. (MANIFEST-REQ-001)
 *
 * <p>Phase1 은 고정 URL 이라 만료 시각({@code urlExpiresAt})을 응답하지 않는다.
 *
 * @param campaignId 캠페인 ID
 * @param targetVersion 설치할 버전
 * @param fileSize 파일 크기(byte)
 * @param sha256 파일의 SHA-256
 * @param downloadUrl 파일을 받을 CDN URL
 */
public record ManifestResponse(
        Long campaignId, String targetVersion, long fileSize, String sha256, String downloadUrl) {

    public static ManifestResponse from(ManifestResult result) {
        return new ManifestResponse(
                result.campaignId(), result.targetVersion(), result.fileSize(), result.sha256(), result.downloadUrl());
    }
}
