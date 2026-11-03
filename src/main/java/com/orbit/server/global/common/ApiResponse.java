package com.orbit.server.global.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.orbit.server.global.error.ErrorCode;

/**
 * 모든 API 가 쓰는 공통 응답 형식. 성공과 실패를 같은 구조로 응답한다.
 *
 * <p>실패 정보는 {@code isSuccess}, {@code code}, {@code message} 로 알린다. {@code error} 는 필드 검증 상세처럼 추가 정보가
 * 필요해질 때 쓰려고 자리만 두었고 지금은 항상 {@code null} 이다.
 *
 * @param isSuccess 요청 성공 여부
 * @param code 성공 코드({@link SuccessCode}) 또는 오류 코드({@link ErrorCode})
 * @param message 사람이 읽는 설명. 분기는 {@code code} 로 한다
 * @param data 성공 시 응답 본문. 실패 시 {@code null}
 * @param error 오류 상세. 지금은 항상 {@code null}
 * @param <T> 응답 본문 타입
 */
@JsonPropertyOrder({"isSuccess", "code", "message", "data", "error"})
public record ApiResponse<T>(
        @JsonProperty("isSuccess") boolean isSuccess, String code, String message, T data, Object error) {

    /** 200 성공 응답을 만든다. */
    public static <T> ApiResponse<T> success(T data) {
        return success(SuccessCode.OK, data);
    }

    /** 지정한 성공 코드로 응답을 만든다. 201 처럼 200 이 아닌 성공에 쓴다. */
    public static <T> ApiResponse<T> success(SuccessCode successCode, T data) {
        return new ApiResponse<>(true, successCode.getCode(), successCode.getMessage(), data, null);
    }

    /** 오류 코드로 실패 응답을 만든다. */
    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null, null);
    }
}
