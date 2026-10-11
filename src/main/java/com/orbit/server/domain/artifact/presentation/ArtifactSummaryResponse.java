package com.orbit.server.domain.artifact.presentation;

import com.orbit.server.domain.artifact.application.ArtifactSummary;
import java.time.Instant;

/**
 * Origin 파일 목록의 한 항목.
 *
 * @param artifactId Artifact 의 ID. 캠페인 등록 때 쓴다
 * @param model 차종
 * @param version 소프트웨어 버전
 * @param fileSize 파일 크기(byte)
 * @param sha256 파일의 SHA-256
 * @param path Origin 경로
 * @param createdAt 등록 시각
 */
public record ArtifactSummaryResponse(
        Long artifactId, String model, String version, long fileSize, String sha256, String path, Instant createdAt) {

    static ArtifactSummaryResponse from(ArtifactSummary summary) {
        return new ArtifactSummaryResponse(
                summary.artifactId(),
                summary.model(),
                summary.version(),
                summary.fileSize(),
                summary.sha256(),
                summary.path(),
                summary.createdAt());
    }
}
