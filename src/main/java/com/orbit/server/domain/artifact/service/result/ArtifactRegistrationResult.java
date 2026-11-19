package com.orbit.server.domain.artifact.service.result;

import com.orbit.server.domain.artifact.domain.ArtifactFile;
import com.orbit.server.domain.artifact.domain.ArtifactSignature;

/**
 * Origin 파일 등록 결과. 서명은 매니페스트로만 내려주므로 키 버전만 담는다.
 *
 * @param artifactId 등록한 Artifact 의 ID. 캠페인 등록 때 이 값으로 파일을 고른다
 * @param fileSize 파일 크기(byte)
 * @param sha256 서버가 계산한 SHA-256
 * @param path Origin 경로
 * @param keyId 서명 검증에 쓴 키 버전
 */
public record ArtifactRegistrationResult(Long artifactId, long fileSize, String sha256, String path, String keyId) {

    public static ArtifactRegistrationResult of(Long artifactId, ArtifactFile file, ArtifactSignature signature) {
        return new ArtifactRegistrationResult(
                artifactId, file.sizeBytes(), file.sha256(), file.path(), signature.keyId());
    }
}
