package com.orbit.server.domain.update.model;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Embeddable;

/**
 * 차량에 설치된 소프트웨어 버전.
 *
 * @param value 버전. 공백 불가, 50자 이하
 */
@Embeddable
public record SoftwareVersion(String value) {

    private static final int MAX_LENGTH = 50;

    /**
     * @throws InvalidValueException 버전이 비어 있거나 50자를 넘는 경우
     */
    public SoftwareVersion {
        Preconditions.requireText(value, MAX_LENGTH, "소프트웨어 버전");
    }
}
