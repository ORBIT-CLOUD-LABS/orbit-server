package com.orbit.server.global.external.origin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.domain.artifact.entity.ArtifactFile;
import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.domain.artifact.exception.OriginFileExistsException;
import com.orbit.server.domain.artifact.exception.OriginUploadException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;

class FileSystemOriginStorageTest {

    private static final byte[] CONTENT = "firmware-binary".getBytes(StandardCharsets.UTF_8);
    // printf 'firmware-binary' | shasum -a 256
    private static final String CONTENT_SHA256 = "4924584e5b634794b60d87f7cfc6ed84086489968f9cfde652c095d41d063ee0";
    private static final String ORIGIN_PATH = "/firmware/model-a/1.0.0/firmware.bin";
    private static final ArtifactFile FILE = new ArtifactFile(CONTENT_SHA256, CONTENT.length, ORIGIN_PATH);

    @TempDir
    private Path root;

    private FileSystemOriginStorage originStorage;

    @BeforeEach
    void setUp() {
        originStorage = new FileSystemOriginStorage(root);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 저장 루트 아래 Origin 경로에 파일을 쓴다")
    void writesFileUnderRoot() throws IOException {
        originStorage.upload(FILE, new ByteArrayResource(CONTENT));

        assertThat(root.resolve("firmware/model-a/1.0.0/firmware.bin")).hasBinaryContent(CONTENT);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    @DisplayName("ORIGIN-POL-001: 서버와 다른 사용자인 Origin 이 읽을 수 있도록 다른 사용자 읽기를 허용한다")
    void writesFileReadableByOthers() throws IOException {
        originStorage.upload(FILE, new ByteArrayResource(CONTENT));

        assertThat(permissionsOf(root.resolve("firmware/model-a/1.0.0/firmware.bin")))
                .isEqualTo("rw-r--r--");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    @DisplayName("ORIGIN-POL-001: 서버와 다른 사용자인 Origin 이 파일까지 찾아갈 수 있도록 새로 만든 디렉터리의 탐색을 허용한다")
    void createsDirectoriesTraversableByOthers() throws IOException {
        originStorage.upload(FILE, new ByteArrayResource(CONTENT));

        assertThat(permissionsOf(root.resolve("firmware"))).isEqualTo("rwxr-xr-x");
        assertThat(permissionsOf(root.resolve("firmware/model-a"))).isEqualTo("rwxr-xr-x");
        assertThat(permissionsOf(root.resolve("firmware/model-a/1.0.0"))).isEqualTo("rwxr-xr-x");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    @DisplayName("ORIGIN-POL-001: 이미 있는 디렉터리는 인프라가 정한 권한이므로 바꾸지 않는다")
    void keepsPermissionsOfExistingDirectories() throws IOException {
        Path firmware = Files.createDirectory(root.resolve("firmware"));
        Files.setPosixFilePermissions(firmware, PosixFilePermissions.fromString("rwx--x--x"));

        originStorage.upload(FILE, new ByteArrayResource(CONTENT));

        assertThat(permissionsOf(firmware)).isEqualTo("rwx--x--x");
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 디렉터리가 있어야 할 자리에 파일이 있으면 업로드 실패로 알린다")
    void throwsUploadFailureWhenFileBlocksDirectory() throws IOException {
        Files.createFile(root.resolve("firmware"));

        assertThatThrownBy(() -> originStorage.upload(FILE, new ByteArrayResource(CONTENT)))
                .isInstanceOf(OriginUploadException.class);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 업로드를 마치면 Origin 이 제공할 수 있는 임시 파일을 남기지 않는다")
    void leavesNoTempFile() throws IOException {
        originStorage.upload(FILE, new ByteArrayResource(CONTENT));

        assertThat(filesIn(root.resolve("firmware/model-a/1.0.0"))).containsExactly("firmware.bin");
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 같은 경로에 파일이 있으면 덮어쓰지 않고 같은 버전이 있다고 알린다")
    void throwsFileExistsWithoutOverwriting() throws IOException {
        originStorage.upload(FILE, new ByteArrayResource(CONTENT));
        byte[] other = "other-binary".getBytes(StandardCharsets.UTF_8);
        // printf 'other-binary' | shasum -a 256
        ArtifactFile otherFile = new ArtifactFile(
                "5bd9a4b4f1356e7b3572ae9f08f431bc6b93da688636719b01f4de95f5d046dd", other.length, ORIGIN_PATH);

        assertThatThrownBy(() -> originStorage.upload(otherFile, new ByteArrayResource(other)))
                .isInstanceOf(OriginFileExistsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.ARTIFACT_VERSION_EXISTS);
        assertThat(root.resolve("firmware/model-a/1.0.0/firmware.bin")).hasBinaryContent(CONTENT);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 쓴 내용의 해시가 계산한 해시와 다르면 파일을 남기지 않고 업로드 실패로 알린다")
    void rejectsContentChangedAfterDigest() throws IOException {
        byte[] changed = "changed-binary!".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> originStorage.upload(FILE, new ByteArrayResource(changed)))
                .isInstanceOf(OriginUploadException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.ORIGIN_UPLOAD_FAILED);
        assertThat(filesIn(root.resolve("firmware/model-a/1.0.0"))).isEmpty();
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

    @Test
    @DisabledOnOs(OS.WINDOWS)
    @DisplayName("ORIGIN-REQ-001: 저장 경로에 쓰기 권한이 없으면 502 ORIGIN_UPLOAD_FAILED 로 알린다")
    void throwsUploadFailureWhenNotWritable() throws IOException {
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("r-xr-xr-x"));
        try {
            assertThatThrownBy(() -> originStorage.upload(FILE, new ByteArrayResource(CONTENT)))
                    .isInstanceOf(OriginUploadException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ArtifactErrorCode.ORIGIN_UPLOAD_FAILED);
        } finally {
            // @TempDir 정리가 실패하지 않도록 권한을 되돌린다
            Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 저장 루트 밖을 가리키는 경로는 쓰지 않고 업로드 실패로 알린다")
    void rejectsPathOutsideRoot() {
        ArtifactFile escaping = new ArtifactFile(CONTENT_SHA256, CONTENT.length, "/firmware/../../firmware.bin");

        assertThatThrownBy(() -> originStorage.upload(escaping, new ByteArrayResource(CONTENT)))
                .isInstanceOf(OriginUploadException.class);
        assertThat(root.resolveSibling("firmware.bin")).doesNotExist();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 등록을 마치지 못한 파일은 올린 경로 그대로 지운다")
    void deletesUploadedFile() {
        originStorage.upload(FILE, new ByteArrayResource(CONTENT));

        originStorage.delete(FILE);

        assertThat(root.resolve("firmware/model-a/1.0.0/firmware.bin")).doesNotExist();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 정리할 파일이 없거나 지우지 못해도 원래 오류를 가리지 않도록 예외를 던지지 않는다")
    void doesNotThrowWhenDeleteFails() {
        ArtifactFile escaping = new ArtifactFile(CONTENT_SHA256, CONTENT.length, "/firmware/../../firmware.bin");

        assertThatCode(() -> originStorage.delete(FILE)).doesNotThrowAnyException();
        assertThatCode(() -> originStorage.delete(escaping)).doesNotThrowAnyException();
    }

    private static String permissionsOf(Path path) throws IOException {
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(path));
    }

    private static List<String> filesIn(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(path -> path.getFileName().toString()).toList();
        }
    }
}
