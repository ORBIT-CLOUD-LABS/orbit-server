package com.orbit.server.domain.campaign.entity;

import com.orbit.server.domain.shared.DottedVersion;
import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;

/**
 * 캠페인이 배포하려는 차종과 목표 버전. 고른 파일과 차종·버전이 모두 같아야 등록할 수 있다.
 *
 * <p>목표 버전은 파일 버전과 같으므로 저장하지 않고, 등록 요청을 검증할 때만 쓴다. 차종까지 비교하는 것은 다른 차종의 파일을 배포해
 * 설치에 실패하는 일을 등록 시점에 막기 위해서다.
 *
 * @param model 대상 차종. 공백 불가, 50자 이하
 * @param version 목표 버전. 공백 불가, 50자 이하, 점으로 나눈 숫자 형식 (예: {@code 1.0.1})
 */
public record TargetRelease(String model, String version) {

    private static final int MAX_LENGTH = 50;
    private static final String VERSION_NAME = "목표 버전";

    /**
     * @throws InvalidValueException 차종이나 목표 버전이 비어 있거나 50자를 넘는 경우, 목표 버전이 점으로 나눈 숫자 형식이 아닌 경우
     */
    // 형식이 틀린 목표 버전이 "파일과 다름" 으로 거부되면 원인을 알기 어려우므로 비교 전에 형식부터 검증한다
    public TargetRelease {
        Preconditions.requireText(model, MAX_LENGTH, "대상 차종");
        DottedVersion.of(Preconditions.requireText(version, MAX_LENGTH, VERSION_NAME), VERSION_NAME);
    }

    /**
     * 고른 파일의 차종·버전과 같은지 검증한다. 버전은 표기가 달라도 비교 규칙상 같으면({@code 1.0}·{@code 1.0.0}) 같은 버전으로
     * 본다. (CAMPAIGN-REQ-001, VEHICLE-POL-001)
     *
     * @param artifactRelease 고른 파일의 차종과 버전
     * @throws InvalidValueException 차종이 다르거나 비교 규칙상 버전이 다른 경우
     */
    public void requireSameAs(TargetRelease artifactRelease) {
        if (!model.equals(artifactRelease.model()) || !isSameVersionAs(artifactRelease)) {
            throw new InvalidValueException("대상 차종·목표 버전이 고른 파일의 차종·버전과 다릅니다.");
        }
    }

    private boolean isSameVersionAs(TargetRelease other) {
        return DottedVersion.of(version, VERSION_NAME).equals(DottedVersion.of(other.version(), VERSION_NAME));
    }
}
