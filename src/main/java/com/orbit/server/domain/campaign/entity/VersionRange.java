package com.orbit.server.domain.campaign.entity;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 캠페인 대상 차량의 현재 버전 범위. 양 끝은 각각 생략할 수 있고, 생략한 쪽은 제한이 없다.
 *
 * <p>버전은 문자열이라 사전순 비교가 틀린다({@code "1.0.10" < "1.0.9"}). 범위 비교는 판정 로직에서 버전 규칙으로 한다.
 *
 * @param min 최소 버전(포함). null 이면 제한 없음, 50자 이하
 * @param max 최대 버전(포함). null 이면 제한 없음, 50자 이하
 */
@Embeddable
public record VersionRange(
        @Column(name = "current_version_min") String min,
        @Column(name = "current_version_max") String max) {

    private static final int MAX_LENGTH = 50;

    /**
     * @throws InvalidValueException 버전이 50자를 넘는 경우
     */
    public VersionRange {
        Preconditions.requireMaxLength(min, MAX_LENGTH, "최소 버전");
        Preconditions.requireMaxLength(max, MAX_LENGTH, "최대 버전");
    }

    /**
     * @return 양 끝 모두 제한이 없는 범위
     */
    public static VersionRange unbounded() {
        return new VersionRange(null, null);
    }

    /**
     * 현재 버전이 범위 안인지 숫자 자리로 비교해 판정한다. 양 끝을 포함한다. (VEHICLE-POL-001)
     *
     * <p>비교해야 하는 버전 중 숫자 자리로 읽을 수 없는 것이 있으면 범위를 판정할 수 없으므로 대상에서 뺀다.
     *
     * @param currentVersion 차량의 현재 버전
     * @return 범위 안이면 true. 제한이 없는 쪽은 비교하지 않는다
     */
    boolean contains(String currentVersion) {
        if (min == null && max == null) {
            return true;
        }
        return DottedVersion.parse(currentVersion)
                .map(current -> isAtLeastMin(current) && isAtMostMax(current))
                .orElse(false);
    }

    private boolean isAtLeastMin(DottedVersion current) {
        if (min == null) {
            return true;
        }
        return DottedVersion.parse(min)
                .map(bound -> current.compareTo(bound) >= 0)
                .orElse(false);
    }

    private boolean isAtMostMax(DottedVersion current) {
        if (max == null) {
            return true;
        }
        return DottedVersion.parse(max)
                .map(bound -> current.compareTo(bound) <= 0)
                .orElse(false);
    }
}
