package com.orbit.server.domain.artifact.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    // 파일 경로로 해석되면 ('.', 'v1') 과 ('v1', '.') 이 모두 /firmware/v1/firmware.bin 이 된다
    @ParameterizedTest
    @ValueSource(strings = {".", ".."})
    @DisplayName("ORIGIN-POL-001: 차종이 '.' 이나 '..' 이면 다른 조합과 경로가 겹칠 수 있어 거부한다")
    void rejectsDotSegmentModel(String model) {
        assertThatThrownBy(() -> new ArtifactRelease(model, "1.0.0")).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {".", ".."})
    @DisplayName("ORIGIN-POL-001: 버전이 '.' 이나 '..' 이면 다른 조합과 경로가 겹칠 수 있어 거부한다")
    void rejectsDotSegmentVersion(String version) {
        assertThatThrownBy(() -> new ArtifactRelease("model-a", version)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: '.' 이 들어간 일반 버전 표기는 허용한다")
    void acceptsDotsInsideValue() {
        ArtifactRelease release = new ArtifactRelease("model.a", "1.0.0-rc.1");

        assertThat(release.originPath()).isEqualTo("/firmware/model.a/1.0.0-rc.1/firmware.bin");
    }
}
