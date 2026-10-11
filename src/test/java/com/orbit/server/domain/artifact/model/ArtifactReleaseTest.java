package com.orbit.server.domain.artifact.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArtifactReleaseTest {

    @Test
    @DisplayName("ORIGIN-POL-001: 차종과 버전으로 버전마다 다른 Origin 경로를 만든다")
    void buildsOriginPathPerVersion() {
        ArtifactRelease release = new ArtifactRelease("model-a", "1.0.0");

        assertThat(release.originPath()).isEqualTo("/firmware/model-a/1.0.0/firmware.bin");
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 차종에 '/' 가 있으면 다른 조합과 경로가 겹칠 수 있어 거부한다")
    void rejectsSlashInModel() {
        assertThatThrownBy(() -> new ArtifactRelease("model/a", "1.0.0")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 버전에 '/' 가 있으면 다른 조합과 경로가 겹칠 수 있어 거부한다")
    void rejectsSlashInVersion() {
        assertThatThrownBy(() -> new ArtifactRelease("model-a", "1.0/0")).isInstanceOf(InvalidValueException.class);
    }
}
