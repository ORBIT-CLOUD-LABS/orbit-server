package com.orbit.server.domain.campaign.domain;

import com.orbit.server.domain.shared.DottedVersion;
import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 캠페인 대상 차량의 현재 버전 범위. 양 끝은 각각 생략할 수 있고, 생략한 쪽은 제한이 없다.
 *
 * <p>버전은 문자열이라 사전순 비교가 틀린다({@code "1.0.10" < "1.0.9"}). 범위 판정과 같은 범위인지 비교는 버전 규칙으로 한다.
 *
 * @param min 최소 버전(포함). null·공백이면 제한 없음(null 로 저장), 50자 이하, 점으로 나눈 숫자 형식
 * @param max 최대 버전(포함). null·공백이면 제한 없음(null 로 저장), 50자 이하, 점으로 나눈 숫자 형식. 양 끝이 다 있으면 최소
 *     버전 이상(같으면 그 버전만 대상)
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
     * @throws InvalidValueException 버전이 50자를 넘거나 점으로 나눈 숫자 형식이 아닌 경우, 최소 버전이 최대 버전보다 큰 경우
     */
    // 화면에서 비워 둔 입력칸은 빈 문자열로 올 수 있다. 대상 조건의 다른 필드(hwVersions·regions)처럼 비어 있으면 제한 없음으로 본다
    public VersionRange {
        min = blankToNull(min);
        max = blankToNull(max);
        requireBound(min, MIN_NAME);
        requireBound(max, MAX_NAME);
        requireOrdered(min, max);
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

    /**
     * 비교 규칙으로 같은 범위인지 판정한다. 표기가 달라도({@code 1.0}·{@code 1.0.0}) 같은 버전이면 같은 끝으로 본다.
     * (CAMPAIGN-POL-001, VEHICLE-POL-001)
     *
     * <p>record 의 {@code equals} 는 입력한 표기를 비교하므로 중복 판정에 쓰지 않는다. 표기는 입력한 그대로 저장·응답한다.
     *
     * @param other 비교할 범위
     * @return 양 끝이 각각 비교 규칙상 같으면 true. 제한이 없는 쪽은 제한이 없는 쪽과만 같다
     */
    boolean isSameRangeAs(VersionRange other) {
        return isSameBound(min, other.min, MIN_NAME) && isSameBound(max, other.max, MAX_NAME);
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

    // 뒤집힌 범위는 어떤 차량도 대상이 되지 않는데 등록은 성공해 관리자가 원인을 알기 어려우므로 입력 단계에서 거부한다
    private static void requireOrdered(String min, String max) {
        if (min == null || max == null) {
            return;
        }
        if (DottedVersion.of(min, MIN_NAME).compareTo(DottedVersion.of(max, MAX_NAME)) > 0) {
            throw new InvalidValueException("최소 버전은 최대 버전보다 클 수 없습니다.");
        }
    }

    private static boolean isSameBound(String bound, String otherBound, String name) {
        if (bound == null || otherBound == null) {
            return bound == null && otherBound == null;
        }
        return DottedVersion.isSame(bound, otherBound, name);
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
