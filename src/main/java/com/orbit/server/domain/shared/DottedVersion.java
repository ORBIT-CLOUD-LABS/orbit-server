package com.orbit.server.domain.shared;

import com.orbit.server.global.error.InvalidValueException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 점으로 나눈 숫자 자리로 비교하는 버전. 문자열 순서로는 {@code "1.0.10" < "1.0.9"} 가 되므로 대상 판정의 버전 비교는 이
 * 타입으로 한다. (VEHICLE-POL-001)
 *
 * <p>빈 자리는 0 으로 보므로 {@code 1.0} 과 {@code 1.0.0} 은 같은 버전이다.
 */
public final class DottedVersion implements Comparable<DottedVersion> {

    // int 범위를 넘지 않도록 한 자리는 9자까지만 받는다
    private static final Pattern FORMAT = Pattern.compile("^\\d{1,9}(\\.\\d{1,9})*$");
    private static final String SEPARATOR = "\\.";

    // 비교와 동등성이 어긋나지 않도록 끝의 0 자리를 뗀 값만 둔다
    private final List<Integer> segments;

    private DottedVersion(List<Integer> segments) {
        this.segments = List.copyOf(segments);
    }

    /**
     * 입력받은 버전 문자열을 읽는다. 형식이 다른 버전이 저장되면 대상 판정에서 그 차량·파일이 오류 없이 빠지므로, 입력 단계에서 거부한다.
     *
     * @param value 버전 문자열. 예: {@code 1.0.10}
     * @param name 오류 메시지에 쓸 값 이름
     * @return 읽은 버전
     * @throws InvalidValueException 점으로 나눈 숫자 형식이 아니거나 한 자리가 9자를 넘는 경우
     */
    public static DottedVersion of(String value, String name) {
        return parse(value)
                .orElseThrow(() -> new InvalidValueException(name + " 은(는) 점으로 나눈 숫자 형식이어야 합니다. (예: 1.0.1)"));
    }

    /**
     * 버전 문자열을 읽는다. 등록된 데이터에 형식이 다른 버전이 있어도 체크인이 실패하지 않도록 예외 대신 빈 값을 돌려준다.
     *
     * @param value 버전 문자열. 예: {@code 1.0.10}
     * @return 숫자 자리로 읽을 수 없으면 빈 값
     */
    public static Optional<DottedVersion> parse(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            return Optional.empty();
        }
        List<Integer> segments = new ArrayList<>(
                Arrays.stream(value.split(SEPARATOR)).map(Integer::valueOf).toList());
        while (segments.getLast() == 0 && segments.size() > 1) {
            segments.removeLast();
        }
        return Optional.of(new DottedVersion(segments));
    }

    @Override
    public int compareTo(DottedVersion other) {
        int length = Math.max(segments.size(), other.segments.size());
        for (int i = 0; i < length; i++) {
            int compared = Integer.compare(segmentAt(i), other.segmentAt(i));
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DottedVersion other && segments.equals(other.segments);
    }

    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    @Override
    public String toString() {
        return String.join(".", segments.stream().map(String::valueOf).toList());
    }

    private int segmentAt(int index) {
        if (index >= segments.size()) {
            return 0;
        }
        return segments.get(index);
    }
}
