package com.orbit.server.domain.artifact.application;

import com.orbit.server.domain.artifact.model.ArtifactFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.core.io.InputStreamSource;

/**
 * 업로드한 파일의 SHA-256 과 크기. 차량이 다운로드한 파일을 검증할 기준값이 된다. (ORIGIN-REQ-001)
 *
 * @param sha256 소문자 16진수 SHA-256
 * @param sizeBytes 파일 크기(byte)
 */
record FileDigest(String sha256, long sizeBytes) {

    private static final String ALGORITHM = "SHA-256";

    /**
     * 파일을 끝까지 한 번 읽어 해시와 크기를 함께 구한다. 펌웨어는 수백 MB 일 수 있어 메모리에 올리지 않고 스트림으로 읽는다.
     *
     * @param content 파일 내용
     * @return 계산한 해시와 크기
     * @throws UncheckedIOException 파일을 읽지 못한 경우
     */
    static FileDigest of(InputStreamSource content) {
        MessageDigest digest = newDigest();
        try (InputStream in = new DigestInputStream(content.getInputStream(), digest)) {
            // 내용은 저장할 필요 없이 읽는 동안 해시만 계산되면 되므로 버린다
            long sizeBytes = in.transferTo(OutputStream.nullOutputStream());
            return new FileDigest(HexFormat.of().formatHex(digest.digest()), sizeBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("업로드한 파일을 읽지 못했습니다.", e);
        }
    }

    /**
     * 계산한 해시·크기로 Origin 경로의 파일 정보를 만든다.
     *
     * @param path Origin 경로
     * @return 파일 정보
     * @throws com.orbit.server.global.error.InvalidValueException 빈 파일인 경우
     */
    ArtifactFile toArtifactFile(String path) {
        return new ArtifactFile(sha256, sizeBytes, path);
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
