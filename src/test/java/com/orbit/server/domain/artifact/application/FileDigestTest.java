package com.orbit.server.domain.artifact.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.domain.artifact.model.ArtifactFile;
import com.orbit.server.global.error.InvalidValueException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class FileDigestTest {

    private static final String PATH = "/firmware/model-a/1.0.0/firmware.bin";

    // echo -n "hello" | sha256sum
    private static final String HELLO_SHA256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    @Test
    @DisplayName("ORIGIN-REQ-001: 파일 내용으로 소문자 SHA-256 과 크기를 계산한다")
    void calculatesSha256AndSize() {
        FileDigest digest = FileDigest.of(new ByteArrayResource("hello".getBytes(StandardCharsets.UTF_8)));

        assertThat(digest).isEqualTo(new FileDigest(HELLO_SHA256, 5));
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 계산한 해시·크기와 Origin 경로로 파일 정보를 만든다")
    void createsArtifactFile() {
        FileDigest digest = new FileDigest(HELLO_SHA256, 5);

        assertThat(digest.toArtifactFile(PATH)).isEqualTo(new ArtifactFile(HELLO_SHA256, 5, PATH));
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 빈 파일은 등록할 수 없다")
    void rejectsEmptyFile() {
        FileDigest digest = FileDigest.of(new ByteArrayResource(new byte[0]));

        assertThatThrownBy(() -> digest.toArtifactFile(PATH)).isInstanceOf(InvalidValueException.class);
    }
}
