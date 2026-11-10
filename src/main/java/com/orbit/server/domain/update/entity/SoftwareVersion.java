package com.orbit.server.domain.update.entity;

import com.orbit.server.domain.shared.DottedVersion;
import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Embeddable;

/**
 * 차량에 설치된 소프트웨어 버전.
 *
 * @param value 버전. 공백 불가, 50자 이하, 점으로 나눈 숫자 형식 (예: {@code 1.0.1})
 */
@Embeddable
public record SoftwareVersion(String value) {

    private static final int MAX_LENGTH = 50;
    private static final String NAME = "소프트웨어 버전";

    /**
     * @throws InvalidValueException 버전이 비어 있거나 50자를 넘거나 점으로 나눈 숫자 형식이 아닌 경우
     */
    public SoftwareVersion {
        Preconditions.requireText(value, MAX_LENGTH, NAME);
        DottedVersion.of(value, NAME);
    }
}
