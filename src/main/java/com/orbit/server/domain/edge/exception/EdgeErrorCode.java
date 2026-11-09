package com.orbit.server.domain.edge.exception;

import com.orbit.server.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * CDN Edge 를 고르는 중 발생하는 오류.
 *
 * <p>HTTP 상태에 의존하므로 Spring 에 의존하지 않는 entity 가 아닌 exception 에 둔다.
 */
@Getter
public enum EdgeErrorCode implements ErrorCode {
    // 서버가 아니라 CDN 쪽 장애라 일시적이다. 차량은 다음 체크인에 다시 시도한다
    NO_AVAILABLE_EDGE(HttpStatus.SERVICE_UNAVAILABLE, "정상인 CDN Edge 가 없습니다.");

    private final HttpStatus status;
    private final String message;

    EdgeErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return name();
    }
}
