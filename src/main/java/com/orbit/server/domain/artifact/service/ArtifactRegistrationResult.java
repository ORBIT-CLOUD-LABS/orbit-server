package com.orbit.server.domain.artifact.service;

import com.orbit.server.domain.artifact.entity.ArtifactFile;

/**
 * Origin 파일 등록 결과.
 *
 * @param artifactId 등록한 Artifact 의 ID. 캠페인 등록 때 이 값으로 파일을 고른다
 * @param fileSize 파일 크기(byte)
 * @param sha256 서버가 계산한 SHA-256
 * @param path Origin 경로
 */
public record ArtifactRegistrationResult(Long artifactId, long fileSize, String sha256, String path) {

    static ArtifactRegistrationResult of(Long artifactId, ArtifactFile file) {
        return new ArtifactRegistrationResult(artifactId, file.sizeBytes(), file.sha256(), file.path());
    }
}
