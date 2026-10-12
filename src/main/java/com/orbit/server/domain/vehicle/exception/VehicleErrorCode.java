package com.orbit.server.domain.vehicle.exception;

import com.orbit.server.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 차량 API 에서 발생하는 오류.
 *
 * <p>HTTP 상태에 의존하므로 Spring 에 의존하지 않는 entity 가 아닌 exception 에 둔다.
 */
@Getter
public enum VehicleErrorCode implements ErrorCode {
    // 유효한 토큰으로 다른 차량의 경로를 호출한 경우. 재등록해도 해결되지 않으므로 401 과 구분한다
    VEHICLE_MISMATCH(HttpStatus.FORBIDDEN, "토큰의 차량과 요청 경로의 차량이 다릅니다.");

    private final HttpStatus status;
    private final String message;

    VehicleErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return name();
    }
}
