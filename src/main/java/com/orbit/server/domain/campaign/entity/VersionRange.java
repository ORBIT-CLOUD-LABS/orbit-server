package com.orbit.server.domain.campaign.entity;

import com.orbit.server.domain.shared.DottedVersion;
import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 캠페인 대상 차량의 현재 버전 범위. 양 끝은 각각 생략할 수 있고, 생략한 쪽은 제한이 없다.
 *
 * <p>버전은 문자열이라 사전순 비교가 틀린다({@code "1.0.10" < "1.0.9"}). 범위 비교는 판정 로직에서 버전 규칙으로 한다.
 *
 * @param min 최소 버전(포함). null·공백이면 제한 없음(null 로 저장), 50자 이하, 점으로 나눈 숫자 형식
 * @param max 최대 버전(포함). null·공백이면 제한 없음(null 로 저장), 50자 이하, 점으로 나눈 숫자 형식
 */
@Embeddable
public record VersionRange(
        @Column(name = "current_version_min") String min,
        @Column(name = "current_version_max") String max) {

    private static final int MAX_LENGTH = 50;
    private static final String MIN_NAME = "최소 버전";
    private static final String MAX_NAME = "최대 버전";
    private static final String CURRENT_NAME = "현재 버전";

    /**
     * @throws InvalidValueException 버전이 50자를 넘거나 점으로 나눈 숫자 형식이 아닌 경우
     */
    // 화면에서 비워 둔 입력칸은 빈 문자열로 올 수 있다. 대상 조건의 다른 필드(hwVersions·regions)처럼 비어 있으면 제한 없음으로 본다
    public VersionRange {
        min = blankToNull(min);
        max = blankToNull(max);
        requireBound(min, MIN_NAME);
        requireBound(max, MAX_NAME);
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
     * @param currentVersion 차량의 현재 버전. 입력 단계에서 형식을 검증해 저장한 값
     * @return 범위 안이면 true. 제한이 없는 쪽은 비교하지 않는다
     * @throws InvalidValueException 현재 버전이 점으로 나눈 숫자 형식이 아닌 경우
     */
    boolean contains(String currentVersion) {
        DottedVersion current = DottedVersion.of(currentVersion, CURRENT_NAME);
        return isAtLeastMin(current) && isAtMostMax(current);
    }

    private static String blankToNull(String bound) {
        if (bound == null || bound.isBlank()) {
            return null;
        }
        return bound;
    }

    // 생략한 쪽(null)은 제한이 없으므로 검사하지 않는다
    private static void requireBound(String bound, String name) {
        if (bound == null) {
            return;
        }
        DottedVersion.of(Preconditions.requireMaxLength(bound, MAX_LENGTH, name), name);
    }

    private boolean isAtLeastMin(DottedVersion current) {
        if (min == null) {
            return true;
        }
        return current.compareTo(DottedVersion.of(min, MIN_NAME)) >= 0;
    }

    private boolean isAtMostMax(DottedVersion current) {
        if (max == null) {
            return true;
        }
        return current.compareTo(DottedVersion.of(max, MAX_NAME)) <= 0;
    }
}
