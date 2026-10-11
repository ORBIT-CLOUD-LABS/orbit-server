package com.orbit.server.domain.artifact.service;

import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.io.InputStreamSource;

/**
 * Origin 파일 등록 요청.
 *
 * @param release 차종과 버전
 * @param content 파일 내용. 해시 계산과 업로드에서 각각 읽는다
 * @param expectedSha256 관리자가 알려 준 SHA-256. 없으면 null 이고 비교하지 않는다
 */
public record RegisterArtifactCommand(ArtifactRelease release, InputStreamSource content, String expectedSha256) {

    public RegisterArtifactCommand {
        Objects.requireNonNull(release, "release");
        Objects.requireNonNull(content, "content");
    }

    /**
     * @return 관리자가 SHA-256 을 보냈으면 그 값
     */
    public Optional<String> findExpectedSha256() {
        return Optional.ofNullable(expectedSha256);
    }
}
