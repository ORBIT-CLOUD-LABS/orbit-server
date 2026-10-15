package com.orbit.server.global.external.origin;

import com.orbit.server.domain.artifact.entity.ArtifactFile;
import com.orbit.server.domain.artifact.exception.OriginFileExistsException;
import com.orbit.server.domain.artifact.exception.OriginUploadException;
import com.orbit.server.domain.artifact.service.OriginStorage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamSource;

/**
 * Origin 원본 저장소(NFS)를 마운트한 경로에 파일을 쓴다. Origin Nginx 는 같은 저장소를 읽기 전용으로 마운트해 같은 경로로 제공한다.
 */
@Slf4j
class FileSystemOriginStorage implements OriginStorage {

    private static final String LEADING_SLASH = "^/";
    private static final String TEMP_PREFIX = ".upload-";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final String ALGORITHM = "SHA-256";
    // 임시 파일은 소유자만 읽을 수 있게(600) 만들어지고 하드링크로 연결한 최종 파일도 같은 권한을 가지므로,
    // 서버와 다른 사용자로 실행되는 Origin Nginx 가 읽을 수 있게 다른 사용자 읽기를 허용한다
    private static final Set<PosixFilePermission> READABLE_BY_ORIGIN = PosixFilePermissions.fromString("rw-r--r--");

    private final Path root;

    /**
     * @param root 저장 루트. Origin 경로는 이 아래의 상대 경로로 둔다
     */
    FileSystemOriginStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void upload(ArtifactFile file, InputStreamSource content) {
        try {
            store(file, content);
        } catch (FileAlreadyExistsException e) {
            log.info("Origin 에 같은 경로의 파일이 있어 업로드하지 않았습니다: path={}", file.path());
            throw new OriginFileExistsException(e);
        } catch (IOException e) {
            throw uploadFailed(file, e);
        }
    }

    @Override
    public void delete(ArtifactFile file) {
        try {
            Files.deleteIfExists(resolve(file));
        } catch (IOException e) {
            // 남은 파일은 같은 경로 쓰기 거부에 걸려 이 버전의 재등록을 막으므로 수동으로 지워야 한다
            log.error("Origin 파일을 정리하지 못했습니다. 수동 삭제가 필요합니다: path={}", file.path(), e);
        }
    }

    // 최종 경로에 바로 쓰면 Origin 이 쓰는 중인 파일을 제공할 수 있으므로, 같은 디렉터리의 임시 파일에 다 쓰고 검증한 뒤 연결한다.
    // rename 은 기존 파일을 말없이 덮어쓰므로, 같은 경로에 파일이 있으면 실패하는 하드링크로 동시 등록 중 늦은 요청을 막는다
    private void store(ArtifactFile file, InputStreamSource content) throws IOException {
        Path target = resolve(file);
        Path directory = Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(directory, TEMP_PREFIX, TEMP_SUFFIX);
        try {
            // 생성 시 권한을 지정하면 umask 가 적용되므로, 만든 뒤 바꿔 실행 환경의 umask 와 관계없이 맞춘다
            Files.setPosixFilePermissions(temp, READABLE_BY_ORIGIN);
            writeVerified(temp, file, content);
            Files.createLink(target, temp);
        } finally {
            deleteTemp(temp);
        }
    }

    // 해시 계산 때와 쓸 때 읽은 바이트가 다르면(읽기 사이 변경) DB 해시와 Origin 파일이 어긋나므로, 쓴 바이트로 다시 확인한다
    private void writeVerified(Path temp, ArtifactFile file, InputStreamSource content) throws IOException {
        MessageDigest digest = newDigest();
        try (InputStream in = new DigestInputStream(content.getInputStream(), digest)) {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        String written = HexFormat.of().formatHex(digest.digest());
        if (!file.matchesSha256(written)) {
            throw new IOException("Origin 에 쓴 파일의 SHA-256 이 계산한 값과 다릅니다: written=" + written);
        }
    }

    // 연결을 마친 뒤에는 임시 파일이 남아도 업로드 결과는 맞으므로, 정리 실패로 업로드를 실패시키지 않는다
    private void deleteTemp(Path temp) {
        try {
            Files.deleteIfExists(temp);
        } catch (IOException e) {
            log.warn("Origin 임시 파일을 지우지 못했습니다: path={}", temp, e);
        }
    }

    // Origin 경로는 '/' 로 시작하지만 저장 루트 기준 상대 경로로 붙인다. '..' 로 루트 밖에 쓰지 않도록 정규화한 결과를 확인한다
    private Path resolve(ArtifactFile file) throws IOException {
        String relative = file.path().replaceFirst(LEADING_SLASH, "");
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IOException("Origin 저장 루트 밖의 경로입니다: path=" + file.path());
        }
        return target;
    }

    // 전역 예외 처리기는 비즈니스 예외의 코드만 남기므로 Origin 장애 원인은 여기서 기록한다
    private OriginUploadException uploadFailed(ArtifactFile file, IOException e) {
        log.warn("Origin 업로드에 실패했습니다: path={}", file.path(), e);
        return new OriginUploadException(e);
    }

    // SHA-256 은 모든 Java 구현이 지원해야 하는 알고리즘이므로 실패는 실행 환경 문제다
    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " 을 지원하지 않는 실행 환경입니다.", e);
        }
    }
}
