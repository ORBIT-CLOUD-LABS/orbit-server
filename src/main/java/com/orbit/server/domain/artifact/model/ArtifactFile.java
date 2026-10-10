package com.orbit.server.domain.artifact.model;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 차량이 다운로드한 파일을 검증할 때 쓰는 파일 정보. 매니페스트로 그대로 내려간다.
 *
 * @param sha256 파일의 SHA-256 해시. 소문자 16진수 64자
 * @param sizeBytes 파일 크기(byte). 1 이상
 * @param path Origin 경로. 공백 불가, 500자 이하
 */
@Embeddable
public record ArtifactFile(
        @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "sha256", length = 64)
        String sha256,

        @Column(name = "size_bytes") long sizeBytes,
        @Column(name = "path") String path) {

    // DB 검사 제약(ck_artifact_sha256)과 같은 형식
    private static final Pattern SHA256_FORMAT = Pattern.compile("^[0-9a-f]{64}$");
    private static final int PATH_MAX_LENGTH = 500;

    /**
     * @throws InvalidValueException 해시 형식이 틀리거나, 크기가 0 이하이거나, 경로가 비어 있거나 너무 긴 경우
     */
    public ArtifactFile {
        if (sha256 == null || !SHA256_FORMAT.matcher(sha256).matches()) {
            throw new InvalidValueException("SHA-256 은 소문자 16진수 64자여야 합니다.");
        }
        if (sizeBytes <= 0) {
            throw new InvalidValueException("파일 크기는 0보다 커야 합니다.");
        }
        Preconditions.requireText(path, PATH_MAX_LENGTH, "Origin 경로");
    }
}
