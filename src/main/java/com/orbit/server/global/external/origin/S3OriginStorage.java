package com.orbit.server.global.external.origin;

import com.orbit.server.domain.artifact.entity.ArtifactFile;
import com.orbit.server.domain.artifact.exception.OriginFileExistsException;
import com.orbit.server.domain.artifact.exception.OriginUploadException;
import com.orbit.server.domain.artifact.service.OriginStorage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** S3 API 로 Origin File Server(SeaweedFS)에 파일을 올린다. */
@Slf4j
@Component
@RequiredArgsConstructor
class S3OriginStorage implements OriginStorage {

    private static final String LEADING_SLASH = "^/";
    private static final String ANY_EXISTING_OBJECT = "*";

    private final S3Client s3Client;
    private final OriginProperties properties;

    @Override
    public void upload(ArtifactFile file, InputStreamSource content) {
        try (InputStream in = content.getInputStream()) {
            // S3 putObject로 SeaweedFS에 업로드
            s3Client.putObject(requestOf(file), RequestBody.fromInputStream(in, file.sizeBytes()));
        } catch (S3Exception e) {
            throw translate(file, e);
        } catch (IOException | SdkException e) {
            throw uploadFailed(file, e);
        }
    }

    @Override
    public void delete(ArtifactFile file) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKeyOf(file))
                .build();
        try {
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            // 남은 파일은 같은 경로 조건부 쓰기에 걸려 이 버전의 재등록을 막으므로 수동으로 지워야 한다
            log.error("Origin 파일을 정리하지 못했습니다. 수동 삭제가 필요합니다: path={}", file.path(), e);
        }
    }

    // If-None-Match 조건에 걸린 412 는 장애가 아니라 같은 경로에 먼저 올린 요청이 있다는 뜻이다
    private RuntimeException translate(ArtifactFile file, S3Exception e) {
        if (e.statusCode() == HttpStatus.PRECONDITION_FAILED.value()) {
            log.info("Origin 에 같은 경로의 파일이 있어 업로드하지 않았습니다: path={}", file.path());
            return new OriginFileExistsException(e);
        }
        return uploadFailed(file, e);
    }

    // 전역 예외 처리기는 비즈니스 예외의 코드만 남기므로 Origin 장애 원인은 여기서 기록한다
    private OriginUploadException uploadFailed(ArtifactFile file, Exception e) {
        log.warn("Origin 업로드에 실패했습니다: path={}", file.path(), e);
        return new OriginUploadException(e);
    }

    // 해시 계산 때와 업로드 때 읽은 바이트가 다르면(읽기 사이 변경, 전송 중 손상) DB 해시와 Origin 파일이 어긋난다.
    // 계산한 해시를 체크섬으로 보내 Origin 이 받은 바이트를 직접 검증하게 하고, 다르면 저장하지 않게 한다
    private PutObjectRequest requestOf(ArtifactFile file) {
        return PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKeyOf(file))
                .contentLength(file.sizeBytes())
                .checksumSHA256(checksumOf(file))
                // 같은 경로에 객체가 있으면 Origin 이 거부해, 동시 등록 중 늦은 요청이 먼저 올린 파일을 덮어쓰지 못하게 한다
                .ifNoneMatch(ANY_EXISTING_OBJECT)
                .build();
    }

    // S3 체크섬 헤더는 16진수가 아닌 Base64 표기를 요구한다
    private String checksumOf(ArtifactFile file) {
        byte[] digest = HexFormat.of().parseHex(file.sha256());
        return Base64.getEncoder().encodeToString(digest);
    }

    // Origin 경로는 문서대로 '/' 로 시작하지만 S3 객체 키는 버킷 기준 상대 경로라 앞의 '/' 를 뗀다
    private String objectKeyOf(ArtifactFile file) {
        String path = file.path();
        return path.replaceFirst(LEADING_SLASH, "");
    }
}
