package com.orbit.server.global.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.info("비즈니스 예외 발생: code={}", errorCode.getCode());
        return respond(errorCode);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        ServletRequestBindingException.class,
        BindException.class,
        ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidInput(Exception e) {
        log.info("요청 값 검증 실패: exception={}", e.getClass().getSimpleName());
        return respond(CommonErrorCode.INVALID_INPUT);
    }

    // IllegalArgumentException 은 서버 버그·프레임워크 내부에서도 던져지므로 400 으로 매핑하지 않고 500 으로 둔다
    @ExceptionHandler(InvalidValueException.class)
    public ResponseEntity<ErrorResponse> handleInvalidValue(InvalidValueException e) {
        log.info("도메인 값 검증 실패: {}", e.getMessage());
        return respond(CommonErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return respond(CommonErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return respond(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        return respond(CommonErrorCode.NOT_ACCEPTABLE);
    }

    // 업로드 크기 제한(spring.servlet.multipart)을 넘은 요청은 서버 오류가 아니므로 500 으로 떨어지지 않게 막는다
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.info("업로드 크기 제한 초과: maxUploadSize={}", e.getMaxUploadSize());
        return respond(CommonErrorCode.CONTENT_TOO_LARGE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
        return respond(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("데이터 무결성 제약 위반으로 요청이 충돌했습니다: exception={}", e.getClass().getSimpleName());
        return respond(CommonErrorCode.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("예상하지 못한 서버 오류가 발생했습니다.", e);
        return respond(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    // 요청이 text/event-stream 처럼 JSON 이 아닌 형식만 받는 API 여도 오류 본문을 비우지 않도록 형식을 정해 둔다.
    // 정하지 않으면 핸들러의 produces 로 협상해 JSON 을 쓸 수 없어 본문 없이 상태 코드만 나간다
    private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(errorCode));
    }
}
