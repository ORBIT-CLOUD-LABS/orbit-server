package com.orbit.server.global.error;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }

    /**
     * 외부 시스템 오류처럼 원인 예외가 있는 경우에 쓴다. 장애 분석을 위해 cause 를 유지한다.
     *
     * @param errorCode 응답할 오류 코드
     * @param cause 원인 예외
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
    }
}
