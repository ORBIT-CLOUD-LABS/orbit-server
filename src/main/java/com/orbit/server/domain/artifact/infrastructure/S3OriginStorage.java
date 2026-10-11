package com.orbit.server.domain.artifact.infrastructure;

import com.orbit.server.domain.artifact.application.OriginStorage;
import com.orbit.server.domain.artifact.application.OriginUploadException;
import com.orbit.server.domain.artifact.model.ArtifactFile;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** S3 API 로 Origin File Server(SeaweedFS)에 파일을 올린다. */
@Slf4j
@Component
@RequiredArgsConstructor
class S3OriginStorage implements OriginStorage {

    private static final String LEADING_SLASH = "^/";

    private final S3Client s3Client;
    private final OriginProperties properties;

    @Override
    public void upload(ArtifactFile file, InputStreamSource content) {
        try (InputStream in = content.getInputStream()) {
            // S3 putObject로 SeaweedFS에 업로드
            s3Client.putObject(requestOf(file), RequestBody.fromInputStream(in, file.sizeBytes()));
        } catch (IOException | SdkException e) {
            // 전역 예외 처리기는 비즈니스 예외의 코드만 남기므로 Origin 장애 원인은 여기서 기록한다
            log.warn("Origin 업로드에 실패했습니다: path={}", file.path(), e);
            throw new OriginUploadException(e);
        }
    }

    private PutObjectRequest requestOf(ArtifactFile file) {
        return PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKeyOf(file))
                .contentLength(file.sizeBytes())
                .build();
    }

    // Origin 경로는 문서대로 '/' 로 시작하지만 S3 객체 키는 버킷 기준 상대 경로라 앞의 '/' 를 뗀다
    private String objectKeyOf(ArtifactFile file) {
        String path = file.path();
        return path.replaceFirst(LEADING_SLASH, "");
    }
}
