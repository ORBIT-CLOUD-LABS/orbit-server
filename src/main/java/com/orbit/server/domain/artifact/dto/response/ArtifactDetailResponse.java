package com.orbit.server.domain.artifact.dto.response;

import com.orbit.server.domain.artifact.service.result.ArtifactSummary;
import java.time.Instant;

/**
 * Origin 파일 상세 응답.
 *
 * <p>지금은 목록 항목과 필드가 같지만, 상세에만 붙는 필드가 생겨도 목록 응답과 따로 바꿀 수 있도록 분리한다.
 *
 * @param artifactId Artifact 의 ID
 * @param model 차종
 * @param version 소프트웨어 버전
 * @param fileSize 파일 크기(byte)
 * @param sha256 파일의 SHA-256
 * @param path Origin 경로
 * @param keyId 서명 키 버전. 서명({@code signature})은 매니페스트로만 내려주므로 넣지 않는다
 * @param createdAt 등록 시각
 */
public record ArtifactDetailResponse(
        Long artifactId,
        String model,
        String version,
        long fileSize,
        String sha256,
        String path,
        String keyId,
        Instant createdAt) {

    public static ArtifactDetailResponse from(ArtifactSummary summary) {
        return new ArtifactDetailResponse(
                summary.artifactId(),
                summary.model(),
                summary.version(),
                summary.fileSize(),
                summary.sha256(),
                summary.path(),
                summary.keyId(),
                summary.createdAt());
    }
}
