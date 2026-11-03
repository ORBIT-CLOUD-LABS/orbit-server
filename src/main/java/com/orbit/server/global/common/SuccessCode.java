package com.orbit.server.global.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 성공 응답의 {@code code}. 오류 코드({@code INVALID_INPUT} 등)와 같이 상수 이름을 코드 값으로 쓴다. */
@Getter
public enum SuccessCode {
    OK(HttpStatus.OK, "요청에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "리소스를 생성했습니다.");

    private final HttpStatus status;
    private final String message;

    SuccessCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * 응답에 나가는 코드 값.
     *
     * @return 상수 이름
     */
    public String getCode() {
        return name();
    }
}
