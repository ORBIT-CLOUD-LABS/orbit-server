package com.orbit.server.domain.artifact.dto.response;

import com.orbit.server.domain.artifact.service.result.ArtifactRegistrationResult;

/**
 * Origin 파일 등록 응답.
 *
 * @param artifactId 등록한 Artifact 의 ID. 캠페인 등록 때 쓴다
 * @param fileSize 파일 크기(byte)
 * @param sha256 서버가 계산한 SHA-256
 * @param path Origin 경로
 * @param keyId 서명 검증에 쓴 키 버전. 서명({@code signature})은 매니페스트로만 내려주므로 넣지 않는다
 */
public record ArtifactRegistrationResponse(Long artifactId, long fileSize, String sha256, String path, String keyId) {

    public static ArtifactRegistrationResponse from(ArtifactRegistrationResult result) {
        return new ArtifactRegistrationResponse(
                result.artifactId(), result.fileSize(), result.sha256(), result.path(), result.keyId());
    }
}
