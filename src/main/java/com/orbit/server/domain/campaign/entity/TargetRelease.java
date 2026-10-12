package com.orbit.server.domain.campaign.entity;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;

/**
 * 캠페인이 배포하려는 차종과 목표 버전. 고른 파일과 차종·버전이 모두 같아야 등록할 수 있다.
 *
 * <p>목표 버전은 파일 버전과 같으므로 저장하지 않고, 등록 요청을 검증할 때만 쓴다. 차종까지 비교하는 것은 다른 차종의 파일을 배포해
 * 설치에 실패하는 일을 등록 시점에 막기 위해서다.
 *
 * @param model 대상 차종. 공백 불가, 50자 이하
 * @param version 목표 버전. 공백 불가, 50자 이하
 */
public record TargetRelease(String model, String version) {

    private static final int MAX_LENGTH = 50;

    /**
     * @throws InvalidValueException 차종이나 목표 버전이 비어 있거나 50자를 넘는 경우
     */
    public TargetRelease {
        Preconditions.requireText(model, MAX_LENGTH, "대상 차종");
        Preconditions.requireText(version, MAX_LENGTH, "목표 버전");
    }

    /**
     * 고른 파일의 차종·버전과 같은지 검증한다. (CAMPAIGN-REQ-001)
     *
     * @param artifactRelease 고른 파일의 차종과 버전
     * @throws InvalidValueException 차종이나 버전이 다른 경우
     */
    public void requireSameAs(TargetRelease artifactRelease) {
        if (!equals(artifactRelease)) {
            throw new InvalidValueException("대상 차종·목표 버전이 고른 파일의 차종·버전과 다릅니다.");
        }
    }
}
