package com.orbit.server.domain.artifact.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.domain.artifact.model.Artifact;
import com.orbit.server.domain.artifact.model.ArtifactFile;
import com.orbit.server.domain.artifact.model.ArtifactRelease;
import com.orbit.server.domain.artifact.model.ArtifactRepository;
import com.orbit.server.global.error.BusinessException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private FakeArtifactRepository artifactRepository;
    private FakeOriginStorage originStorage;
    private ArtifactRegistrationService service;

    @BeforeEach
    void setUp() {
        artifactRepository = new FakeArtifactRepository();
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
        assertThat(artifactRepository.saved).hasSize(1);
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
        assertThat(artifactRepository.saved).isEmpty();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 같은 차종·버전이 있으면 409 ARTIFACT_VERSION_EXISTS 이고 기존 Origin 파일을 덮어쓰지 않는다")
    void rejectsExistingRelease() {
        artifactRepository.releases.add(RELEASE);

        assertThatThrownBy(() -> service.register(new RegisterArtifactCommand(RELEASE, CONTENT, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.ARTIFACT_VERSION_EXISTS);
        assertThat(originStorage.uploaded).isEmpty();
        assertThat(artifactRepository.saved).isEmpty();
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: Origin 업로드에 실패하면 502 ORIGIN_UPLOAD_FAILED 이고 Artifact 를 저장하지 않는다")
    void doesNotSaveWhenUploadFails() {
        originStorage.failing = true;

        assertThatThrownBy(() -> service.register(new RegisterArtifactCommand(RELEASE, CONTENT, null)))
                .isInstanceOf(OriginUploadException.class);
        assertThat(artifactRepository.saved).isEmpty();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 업로드 후 저장에 실패하면 올린 파일을 지워 같은 버전을 다시 등록할 수 있게 하고 원래 오류를 알린다")
    void deletesUploadedFileWhenSaveFails() {
        artifactRepository.saveFailure = new IllegalStateException("db unavailable");

        assertThatThrownBy(() -> service.register(new RegisterArtifactCommand(RELEASE, CONTENT, null)))
                .isSameAs(artifactRepository.saveFailure);
        assertThat(originStorage.deleted).containsExactly(new ArtifactFile(HELLO_SHA256, 5, PATH));
    }

    private static class FakeArtifactRepository implements ArtifactRepository {

        private final Set<ArtifactRelease> releases = new HashSet<>();
        private final List<Artifact> saved = new ArrayList<>();
        private RuntimeException saveFailure;

        @Override
        public boolean existsByRelease(ArtifactRelease release) {
            return releases.contains(release);
        }

        @Override
        public Artifact save(Artifact artifact) {
            if (saveFailure != null) {
                throw saveFailure;
            }
            // ID 는 DB 가 채우므로 저장 순서대로 흉내 낸다
            ReflectionTestUtils.setField(artifact, "id", saved.size() + 1L);
            saved.add(artifact);
            return artifact;
        }
    }

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
