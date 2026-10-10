package com.orbit.server.domain.artifact.model;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 업데이트 파일이 어떤 차종의 어떤 버전인지 나타낸다. 차종과 버전 조합은 Artifact 마다 유일하다.
 *
 * @param model 차종. 공백 불가, 50자 이하
 * @param version 소프트웨어 버전. 공백 불가, 50자 이하
 */
@Embeddable
public record ArtifactRelease(
        @Column(name = "model") String model,
        @Column(name = "version") String version) {

    private static final int MAX_LENGTH = 50;

    /**
     * @throws InvalidValueException 차종이나 버전이 비어 있거나 50자를 넘는 경우
     */
    public ArtifactRelease {
        Preconditions.requireText(model, MAX_LENGTH, "차종");
        Preconditions.requireText(version, MAX_LENGTH, "버전");
    }
}
