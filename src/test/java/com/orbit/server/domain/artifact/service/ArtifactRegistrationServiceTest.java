package com.orbit.server.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orbit.server.domain.artifact.domain.Artifact;
import com.orbit.server.domain.artifact.domain.ArtifactFile;
import com.orbit.server.domain.artifact.domain.ArtifactRelease;
import com.orbit.server.domain.artifact.domain.ArtifactSignature;
import com.orbit.server.domain.artifact.domain.FirmwareSignaturePayload;
import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.domain.artifact.exception.OriginUploadException;
import com.orbit.server.domain.artifact.repository.ArtifactRepository;
import com.orbit.server.domain.artifact.service.command.RegisterArtifactCommand;
import com.orbit.server.domain.artifact.service.result.ArtifactRegistrationResult;
import com.orbit.server.global.error.BusinessException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.test.util.ReflectionTestUtils;

class ArtifactRegistrationServiceTest {

    private static final ArtifactRelease RELEASE = new ArtifactRelease("model-a", "1.0.0");
    private static final InputStreamSource CONTENT = new ByteArrayResource("hello".getBytes(StandardCharsets.UTF_8));
    private static final String PATH = "/firmware/model-a/1.0.0/firmware.bin";

    // echo -n "hello" | sha256sum
    private static final String HELLO_SHA256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    private static final String KEY_ID = "v1";

    private final TestFirmwareSigner signer = new TestFirmwareSigner();
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
        FirmwareSignatureVerifier signatureVerifier =
                new FirmwareSignatureVerifier(FirmwarePublicKeys.fromBase64(Map.of(KEY_ID, signer.publicKeyBase64())));
        service = new ArtifactRegistrationService(artifactRepository, originStorage, signatureVerifier);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 파일을 Origin 경로에 올리고 계산한 해시·크기로 등록한다")
    void registersArtifact() {
        ArtifactRegistrationResult result = service.register(command(null));

        ArtifactFile expectedFile = new ArtifactFile(HELLO_SHA256, 5, PATH);
        assertThat(result).isEqualTo(new ArtifactRegistrationResult(1L, 5, HELLO_SHA256, PATH, KEY_ID));
        assertThat(originStorage.uploaded).containsExactly(expectedFile);
        verify(artifactRepository).save(any());
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 검증한 펌웨어 서명과 키 버전을 Artifact 에 함께 저장한다")
    void savesVerifiedSignature() {
        service.register(command(null));

        ArgumentCaptor<Artifact> saved = ArgumentCaptor.forClass(Artifact.class);
        verify(artifactRepository).save(saved.capture());
        assertThat(saved.getValue().getSignature()).isEqualTo(new ArtifactSignature(signHello(), KEY_ID));
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 서명이 맞지 않으면 422 SIGNATURE_INVALID 이고 Origin 에 올리지 않고 저장하지 않는다")
    void rejectsInvalidSignatureBeforeUpload() {
        String signatureOfOtherKey =
                new TestFirmwareSigner().sign(helloPayload().toBytes());
        RegisterArtifactCommand command =
                new RegisterArtifactCommand(RELEASE, CONTENT, null, signatureOfOtherKey, KEY_ID);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.SIGNATURE_INVALID);
        assertThat(originStorage.uploaded).isEmpty();
        verify(artifactRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "vault-output", "unknown-key-id"})
    @DisplayName("ORIGIN-POL-001: 서명 누락·형식 오류·모르는 키 버전은 파일을 읽어 해시를 계산하기 전에 422 SIGNATURE_INVALID")
    void rejectsSignatureBeforeReadingFile(String invalidCase) {
        // 파일을 열면 실패하는 출처라, 해시 계산 전에 거르지 못하면 SIGNATURE_INVALID 가 아닌 다른 예외가 난다
        InputStreamSource unreadable = () -> {
            throw new IOException("file must not be read");
        };
        String signature = signHello();
        RegisterArtifactCommand command = switch (invalidCase) {
            case "missing" -> new RegisterArtifactCommand(RELEASE, unreadable, null, null, null);
            case "vault-output" ->
                new RegisterArtifactCommand(RELEASE, unreadable, null, "vault:v1:" + signature, KEY_ID);
            default -> new RegisterArtifactCommand(RELEASE, unreadable, null, signature, "v2");
        };

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.SIGNATURE_INVALID);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 요청 해시가 계산한 해시와 같으면 등록한다")
    void registersWhenExpectedSha256Matches() {
        RegisterArtifactCommand command = command(HELLO_SHA256.toUpperCase());

        ArtifactRegistrationResult result = service.register(command);

        assertThat(result.sha256()).isEqualTo(HELLO_SHA256);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 요청 해시가 계산한 해시와 다르면 422 HASH_MISMATCH 이고 업로드·저장하지 않는다")
    void rejectsHashMismatch() {
        RegisterArtifactCommand command = command("b".repeat(64));

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.HASH_MISMATCH);
        assertThat(originStorage.uploaded).isEmpty();
        verify(artifactRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0.0", "1.0", "01.0.0"})
    @DisplayName("ORIGIN-POL-001: 같은 차종에 비교 규칙상 같은 버전이 있으면 409 ARTIFACT_VERSION_EXISTS 이고 Origin 에 올리지 않는다")
    void rejectsExistingRelease(String registeredVersion) {
        given(artifactRepository.findVersionsByModel(RELEASE.model())).willReturn(List.of("0.9", registeredVersion));

        assertThatThrownBy(() -> service.register(command(null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.ARTIFACT_VERSION_EXISTS);
        assertThat(originStorage.uploaded).isEmpty();
        verify(artifactRepository, never()).save(any());
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 같은 차종의 다른 버전만 있으면 등록한다")
    void registersNewVersionOfModel() {
        given(artifactRepository.findVersionsByModel(RELEASE.model())).willReturn(List.of("0.9", "1.0.0.1"));

        service.register(command(null));

        verify(artifactRepository).save(any());
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: Origin 업로드에 실패하면 502 ORIGIN_UPLOAD_FAILED 이고 Artifact 를 저장하지 않는다")
    void doesNotSaveWhenUploadFails() {
        originStorage.failing = true;

        assertThatThrownBy(() -> service.register(command(null))).isInstanceOf(OriginUploadException.class);
        verify(artifactRepository, never()).save(any());
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 업로드 후 저장에 실패하면 올린 파일을 지워 같은 버전을 다시 등록할 수 있게 하고 원래 오류를 알린다")
    void deletesUploadedFileWhenSaveFails() {
        IllegalStateException saveFailure = new IllegalStateException("db unavailable");
        // setUp 의 저장 응답을 실행하지 않고 덮어쓰도록 willThrow(...).given(...) 형식으로 쓴다
        willThrow(saveFailure).given(artifactRepository).save(any());

        assertThatThrownBy(() -> service.register(command(null))).isSameAs(saveFailure);
        assertThat(originStorage.deleted).containsExactly(new ArtifactFile(HELLO_SHA256, 5, PATH));
    }

    // "hello" 파일의 해시·크기로 만든 서명 대상에 서명한 요청
    private RegisterArtifactCommand command(String expectedSha256) {
        return new RegisterArtifactCommand(RELEASE, CONTENT, expectedSha256, signHello(), KEY_ID);
    }

    private String signHello() {
        return signer.sign(helloPayload().toBytes());
    }

    private static FirmwareSignaturePayload helloPayload() {
        return new FirmwareSignaturePayload(RELEASE, new ArtifactFile(HELLO_SHA256, 5, PATH));
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
