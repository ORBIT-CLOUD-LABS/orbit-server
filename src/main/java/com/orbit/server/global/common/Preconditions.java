package com.orbit.server.global.common;

import com.orbit.server.global.error.InvalidValueException;

/**
 * 값 객체 생성자에서 쓰는 입력 검증 도구.
 *
 * <p>컬럼 길이를 넘는 값이 DB 에서 잘리거나 오류가 나기 전에, 생성 시점에 막기 위해 둔다.
 */
public final class Preconditions {

    private Preconditions() {}

    /**
     * 비어 있지 않고 최대 길이 이하인 문자열인지 검증한다.
     *
     * @param value 검증할 값
     * @param maxLength 허용하는 최대 길이
     * @param name 오류 메시지에 쓸 값 이름
     * @return 검증한 값
     * @throws InvalidValueException 값이 null·공백이거나 최대 길이를 넘는 경우
     */
    public static String requireText(String value, int maxLength, String name) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException(name + " 은(는) 비어 있을 수 없습니다.");
        }
        return requireMaxLength(value, maxLength, name);
    }

    /**
     * 값이 있으면 최대 길이 이하인지 검증한다. null 은 허용한다.
     *
     * @param value 검증할 값. null 가능
     * @param maxLength 허용하는 최대 길이
     * @param name 오류 메시지에 쓸 값 이름
     * @return 검증한 값
     * @throws InvalidValueException 값이 최대 길이를 넘는 경우
     */
    public static String requireMaxLength(String value, int maxLength, String name) {
        if (value != null && value.length() > maxLength) {
            throw new InvalidValueException(name + " 은(는) " + maxLength + "자를 넘을 수 없습니다.");
        }
        return value;
    }
}
