package com.orbit.server.global.security;

import com.orbit.server.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 요청 인증에 실패했을 때의 오류. */
@Getter
public enum AuthErrorCode implements ErrorCode {
    // 차량은 이 코드를 받으면 재시도하지 않고 시작 실패로 종료한다 (VEHICLE-POL-003)
    INVALID_ENROLLMENT_KEY(HttpStatus.UNAUTHORIZED, "차량 등록 키가 올바르지 않습니다."),
    // 차량은 이 코드를 받으면 등록부터 다시 한다. 토큰이 없는지 폐기됐는지 구분하지 않아 토큰 상태를 알려주지 않는다
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 토큰이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;

    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return name();
    }
}
