package com.orbit.server.domain.artifact.dto.response;

import com.orbit.server.domain.artifact.service.ArtifactSummary;
import java.time.Instant;

/**
 * Origin 파일 상세 응답.
 *
 * <p>지금은 목록 항목과 필드가 같지만, Phase2 의 서명처럼 상세에만 붙는 필드가 생겨도 목록 응답과 따로 바꿀 수 있도록 분리한다.
 *
 * @param artifactId Artifact 의 ID
 * @param model 차종
 * @param version 소프트웨어 버전
 * @param fileSize 파일 크기(byte)
 * @param sha256 파일의 SHA-256
 * @param path Origin 경로
 * @param createdAt 등록 시각
 */
public record ArtifactDetailResponse(
        Long artifactId, String model, String version, long fileSize, String sha256, String path, Instant createdAt) {

    public static ArtifactDetailResponse from(ArtifactSummary summary) {
        return new ArtifactDetailResponse(
                summary.artifactId(),
                summary.model(),
                summary.version(),
                summary.fileSize(),
                summary.sha256(),
                summary.path(),
                summary.createdAt());
    }
}
