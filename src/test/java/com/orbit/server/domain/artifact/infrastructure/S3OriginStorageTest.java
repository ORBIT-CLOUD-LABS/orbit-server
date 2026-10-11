package com.orbit.server.domain.artifact.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.orbit.server.domain.artifact.application.ArtifactErrorCode;
import com.orbit.server.domain.artifact.application.OriginFileExistsException;
import com.orbit.server.domain.artifact.application.OriginUploadException;
import com.orbit.server.domain.artifact.model.ArtifactFile;
import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3OriginStorageTest {

    private static final OriginProperties PROPERTIES =
            new OriginProperties(URI.create("http://localhost:8333"), "us-east-1", "orbit-firmware", "key", "secret");
    private static final byte[] CONTENT = {1, 2, 3};
    private static final ArtifactFile FILE =
            new ArtifactFile("a".repeat(64), CONTENT.length, "/firmware/model-a/1.0.0/firmware.bin");

    private S3Client s3Client;
    private S3OriginStorage originStorage;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        originStorage = new S3OriginStorage(s3Client, PROPERTIES);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 설정한 버킷에 Origin 경로에서 앞의 '/' 를 뗀 키로 올린다")
    void uploadsToBucketWithObjectKey() {
        originStorage.upload(FILE, new ByteArrayResource(CONTENT));

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue())
                .returns("orbit-firmware", PutObjectRequest::bucket)
                .returns("firmware/model-a/1.0.0/firmware.bin", PutObjectRequest::key)
                .returns((long) CONTENT.length, PutObjectRequest::contentLength);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 계산한 SHA-256 을 Base64 체크섬으로 보내 Origin 이 받은 바이트를 검증하게 한다")
    void sendsSha256Checksum() {
        originStorage.upload(FILE, new ByteArrayResource(CONTENT));

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        // 0xaa 32바이트의 Base64: python3 -c "import base64;print(base64.b64encode(bytes.fromhex('aa'*32)).decode())"
        assertThat(request.getValue().checksumSHA256()).isEqualTo("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqo=");
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 같은 경로에 객체가 있으면 거부하도록 If-None-Match 조건을 붙인다")
    void sendsIfNoneMatchCondition() {
        originStorage.upload(FILE, new ByteArrayResource(CONTENT));

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().ifNoneMatch()).isEqualTo("*");
    }

    @Test
    @DisplayName("ORIGIN-POL-001: Origin 이 412 로 거부하면 같은 버전이 있다고 알린다")
    void throwsFileExistsWhenPreconditionFailed() {
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(s3ExceptionOf(412));

        assertThatThrownBy(() -> originStorage.upload(FILE, new ByteArrayResource(CONTENT)))
                .isInstanceOf(OriginFileExistsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.ARTIFACT_VERSION_EXISTS);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: Origin 이 412 외의 오류로 거부하면 업로드 실패로 알린다")
    void throwsUploadFailureWhenOriginRejects() {
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(s3ExceptionOf(500));

        assertThatThrownBy(() -> originStorage.upload(FILE, new ByteArrayResource(CONTENT)))
                .isInstanceOf(OriginUploadException.class);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: Origin 이 요청을 처리하지 못하면 원인을 담아 업로드 실패로 알린다")
    void throwsUploadFailureWhenOriginFails() {
        SdkClientException cause = SdkClientException.create("connection refused");
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(cause);

        assertThatThrownBy(() -> originStorage.upload(FILE, new ByteArrayResource(CONTENT)))
                .isInstanceOf(OriginUploadException.class)
                .hasCause(cause);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 업로드한 파일을 읽지 못하면 업로드 실패로 알린다")
    void throwsUploadFailureWhenContentUnreadable() {
        InputStreamSource unreadable = () -> {
            throw new IOException("temp file removed");
        };

        assertThatThrownBy(() -> originStorage.upload(FILE, unreadable))
                .isInstanceOf(OriginUploadException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    private static S3Exception s3ExceptionOf(int statusCode) {
        return (S3Exception) S3Exception.builder().statusCode(statusCode).build();
    }
}
