package com.orbit.server.global.error;

/**
 * 값 객체가 생성 시점의 검증 규칙을 어겼을 때 던지는 예외.
 *
 * <p>{@link IllegalArgumentException} 은 서버 코드·프레임워크·JDK 에서도 던져지므로, 클라이언트 입력 오류(400)로
 * 응답해도 되는 경우만 이 예외로 구분한다. 도메인 모델이 Spring 에 의존하지 않도록 {@link BusinessException} 을 상속하지
 * 않는다.
 */
public class InvalidValueException extends RuntimeException {

    /**
     * @param message 어떤 값이 어떤 규칙을 어겼는지
     */
    public InvalidValueException(String message) {
        super(message);
    }
}
