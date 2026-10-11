package com.orbit.server.domain.artifact.application;

import com.orbit.server.domain.artifact.model.Artifact;
import com.orbit.server.domain.artifact.model.ArtifactFile;
import com.orbit.server.domain.artifact.model.ArtifactRelease;
import com.orbit.server.domain.artifact.model.ArtifactRepository;
import com.orbit.server.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 관리자가 올린 업데이트 파일을 Origin 에 저장하고 Artifact 로 등록한다. (ORIGIN-REQ-001 / ORIGIN-POL-001)
 *
 * <p>메서드 전체를 트랜잭션으로 묶지 않는다. 수백 MB 파일의 해시 계산과 업로드 동안 DB 커넥션을 붙잡지 않기 위해서다. 쓰기는
 * 마지막 저장 한 번뿐이라 Repository 의 기본 트랜잭션으로 충분하다.
 */
@Service
@RequiredArgsConstructor
public class ArtifactRegistrationService {

    private final ArtifactRepository artifactRepository;
    private final OriginStorage originStorage;

    /**
     * 중복 확인 → 해시 계산·비교 → Origin 업로드 → 저장 순서로 등록한다. 어느 단계에서든 실패하면 Artifact 를 저장하지 않으므로
     * 캠페인에 연결할 수 없다.
     *
     * @param command 등록 요청
     * @return 등록한 Artifact 의 ID 와 파일 정보
     * @throws BusinessException 같은 차종·버전이 있으면 ARTIFACT_VERSION_EXISTS, 해시가 다르면 HASH_MISMATCH
     * @throws OriginUploadException Origin 업로드에 실패한 경우
     */
    public ArtifactRegistrationResult register(RegisterArtifactCommand command) {
        ArtifactRelease release = command.release();
        // 업로드보다 먼저 확인해 이미 등록된 버전의 Origin 파일을 덮어쓰지 않는다
        requireNewRelease(release);
        ArtifactFile file = digest(command);
        requireSameSha256(file, command);
        originStorage.upload(file, command.content());
        Artifact artifact = artifactRepository.save(new Artifact(release, file));
        return ArtifactRegistrationResult.of(artifact.id(), file);
    }

    private void requireNewRelease(ArtifactRelease release) {
        if (artifactRepository.existsByRelease(release)) {
            throw new BusinessException(ArtifactErrorCode.ARTIFACT_VERSION_EXISTS);
        }
    }

    private ArtifactFile digest(RegisterArtifactCommand command) {
        FileDigest digest = FileDigest.of(command.content());
        ArtifactRelease release = command.release();
        return digest.toArtifactFile(release.originPath());
    }

    private void requireSameSha256(ArtifactFile file, RegisterArtifactCommand command) {
        boolean mismatched = command.findExpectedSha256()
                .filter(expected -> !file.matchesSha256(expected))
                .isPresent();
        if (mismatched) {
            throw new BusinessException(ArtifactErrorCode.HASH_MISMATCH);
        }
    }
}
