package com.orbit.server.global.error;

import org.springframework.http.HttpStatus;

/**
 * 오류 응답의 코드·HTTP 상태·메시지. 컨텍스트마다 enum 으로 구현한다.
 */
public interface ErrorCode {

    /**
     * @return enum 상수 이름. 구현 enum 이 자동으로 제공한다
     */
    String name();

    /**
     * @return 응답 본문의 오류 코드. 상수 이름을 그대로 써서 코드와 상수가 어긋나지 않게 한다
     */
    default String getCode() {
        return name();
    }

    HttpStatus getStatus();

    String getMessage();
}
