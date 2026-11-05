package com.orbit.server.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactFile;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.domain.artifact.exception.OriginUploadException;
import com.orbit.server.domain.artifact.repository.ArtifactRepository;
import com.orbit.server.global.error.BusinessException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.test.util.ReflectionTestUtils;

class ArtifactRegistrationServiceTest {

    private static final ArtifactRelease RELEASE = new ArtifactRelease("model-a", "1.0.0");
    private static final InputStreamSource CONTENT = new ByteArrayResource("hello".getBytes(StandardCharsets.UTF_8));
    private static final String PATH = "/firmware/model-a/1.0.0/firmware.bin";

    // echo -n "hello" | sha256sum
    private static final String HELLO_SHA256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    private ArtifactRepository artifactRepository;
    private FakeOriginStorage originStorage;
    private ArtifactRegistrationService service;

    @BeforeEach
    void setUp() {
        artifactRepository = mock(ArtifactRepository.class);
        // ID 는 DB 가 채우므로 저장할 때 흉내 낸다
        given(artifactRepository.save(any())).willAnswer(invocation -> {
            Artifact artifact = invocation.getArgument(0);
            ReflectionTestUtils.setField(artifact, "id", 1L);
            return artifact;
        });
        originStorage = new FakeOriginStorage();
        service = new ArtifactRegistrationService(artifactRepository, originStorage);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 파일을 Origin 경로에 올리고 계산한 해시·크기로 등록한다")
    void registersArtifact() {
        ArtifactRegistrationResult result = service.register(new RegisterArtifactCommand(RELEASE, CONTENT, null));

        ArtifactFile expectedFile = new ArtifactFile(HELLO_SHA256, 5, PATH);
        assertThat(result).isEqualTo(new ArtifactRegistrationResult(1L, 5, HELLO_SHA256, PATH));
        assertThat(originStorage.uploaded).containsExactly(expectedFile);
        verify(artifactRepository).save(any());
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 요청 해시가 계산한 해시와 같으면 등록한다")
    void registersWhenExpectedSha256Matches() {
        RegisterArtifactCommand command = new RegisterArtifactCommand(RELEASE, CONTENT, HELLO_SHA256.toUpperCase());

        ArtifactRegistrationResult result = service.register(command);

        assertThat(result.sha256()).isEqualTo(HELLO_SHA256);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 요청 해시가 계산한 해시와 다르면 422 HASH_MISMATCH 이고 업로드·저장하지 않는다")
    void rejectsHashMismatch() {
        RegisterArtifactCommand command = new RegisterArtifactCommand(RELEASE, CONTENT, "b".repeat(64));

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.HASH_MISMATCH);
        assertThat(originStorage.uploaded).isEmpty();
        verify(artifactRepository, never()).save(any());
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 같은 차종·버전이 있으면 409 ARTIFACT_VERSION_EXISTS 이고 기존 Origin 파일을 덮어쓰지 않는다")
    void rejectsExistingRelease() {
        given(artifactRepository.existsByRelease(RELEASE)).willReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterArtifactCommand(RELEASE, CONTENT, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.ARTIFACT_VERSION_EXISTS);
        assertThat(originStorage.uploaded).isEmpty();
        verify(artifactRepository, never()).save(any());
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: Origin 업로드에 실패하면 502 ORIGIN_UPLOAD_FAILED 이고 Artifact 를 저장하지 않는다")
    void doesNotSaveWhenUploadFails() {
        originStorage.failing = true;

        assertThatThrownBy(() -> service.register(new RegisterArtifactCommand(RELEASE, CONTENT, null)))
                .isInstanceOf(OriginUploadException.class);
        verify(artifactRepository, never()).save(any());
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 업로드 후 저장에 실패하면 올린 파일을 지워 같은 버전을 다시 등록할 수 있게 하고 원래 오류를 알린다")
    void deletesUploadedFileWhenSaveFails() {
        IllegalStateException saveFailure = new IllegalStateException("db unavailable");
        // setUp 의 저장 응답을 실행하지 않고 덮어쓰도록 willThrow(...).given(...) 형식으로 쓴다
        willThrow(saveFailure).given(artifactRepository).save(any());

        assertThatThrownBy(() -> service.register(new RegisterArtifactCommand(RELEASE, CONTENT, null)))
                .isSameAs(saveFailure);
        assertThat(originStorage.deleted).containsExactly(new ArtifactFile(HELLO_SHA256, 5, PATH));
    }

    // Origin 은 외부 저장소 포트라 호출 기록과 실패를 직접 흉내 낸다
    private static class FakeOriginStorage implements OriginStorage {

        private final List<ArtifactFile> uploaded = new ArrayList<>();
        private final List<ArtifactFile> deleted = new ArrayList<>();
        private boolean failing;

        @Override
        public void upload(ArtifactFile file, InputStreamSource content) {
            if (failing) {
                throw new OriginUploadException(new IOException("origin unavailable"));
            }
            uploaded.add(file);
        }

        @Override
        public void delete(ArtifactFile file) {
            deleted.add(file);
        }
    }
}
