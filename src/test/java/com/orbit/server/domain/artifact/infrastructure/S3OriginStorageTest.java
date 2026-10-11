package com.orbit.server.domain.artifact.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}
