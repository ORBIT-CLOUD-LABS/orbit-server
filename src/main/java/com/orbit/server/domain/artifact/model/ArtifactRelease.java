package com.orbit.server.domain.artifact.model;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 업데이트 파일이 어떤 차종의 어떤 버전인지 나타낸다. 차종과 버전 조합은 Artifact 마다 유일하다.
 *
 * @param model 차종. 공백 불가, 50자 이하, '/' 불가
 * @param version 소프트웨어 버전. 공백 불가, 50자 이하, '/' 불가
 */
@Embeddable
public record ArtifactRelease(
        @Column(name = "model") String model,
        @Column(name = "version") String version) {

    private static final int MAX_LENGTH = 50;
    private static final String PATH_SEPARATOR = "/";
    private static final String ORIGIN_PATH_FORMAT = "/firmware/%s/%s/firmware.bin";

    /**
     * @throws InvalidValueException 차종이나 버전이 비어 있거나 50자를 넘거나 '/' 를 포함하는 경우
     */
    public ArtifactRelease {
        requirePathSegment(Preconditions.requireText(model, MAX_LENGTH, "차종"), "차종");
        requirePathSegment(Preconditions.requireText(version, MAX_LENGTH, "버전"), "버전");
    }

    /**
     * 이 차종·버전의 파일을 둘 Origin 경로. 등록한 파일은 수정하지 않으므로 버전마다 새 경로를 쓴다. (ORIGIN-POL-001)
     *
     * @return {@code /firmware/{model}/{version}/firmware.bin}
     */
    public String originPath() {
        return ORIGIN_PATH_FORMAT.formatted(model, version);
    }

    // '/' 를 허용하면 다른 차종·버전 조합이 같은 경로를 만들어 기존 파일을 덮어쓸 수 있다
    private static void requirePathSegment(String value, String name) {
        if (value.contains(PATH_SEPARATOR)) {
            throw new InvalidValueException(name + " 은(는) '/' 를 포함할 수 없습니다.");
        }
    }
}
