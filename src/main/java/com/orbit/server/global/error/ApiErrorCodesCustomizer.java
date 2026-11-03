package com.orbit.server.global.error;

import io.swagger.v3.oas.models.Operation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/** 컨트롤러 메서드의 {@link ApiErrorCodes} 를 읽어 API 문서에 오류 응답을 HTTP 상태별로 표시한다. */
@Component
class ApiErrorCodesCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        Map<HttpStatusCode, List<ErrorCode>> errorCodesByStatus =
                errorCodesOf(handlerMethod).stream().collect(Collectors.groupingBy(ErrorCode::getStatus));
        errorCodesByStatus
                .values()
                .forEach(errorCodes -> ErrorApiResponses.addTo(operation, errorCodes.toArray(ErrorCode[]::new)));
        return operation;
    }

    private static List<ErrorCode> errorCodesOf(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.findMergedRepeatableAnnotations(handlerMethod.getMethod(), ApiErrorCodes.class)
                .stream()
                .flatMap(annotation -> Arrays.stream(annotation.codes()).map(code -> resolve(annotation.type(), code)))
                .toList();
    }

    private static ErrorCode resolve(Class<? extends ErrorCode> type, String code) {
        if (!type.isEnum()) {
            throw new InvalidValueException(type.getSimpleName() + " 은(는) 오류 코드 enum 이 아닙니다.");
        }
        return Arrays.stream(type.getEnumConstants())
                .filter(errorCode -> errorCode.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new InvalidValueException(type.getSimpleName() + " 에 " + code + " 오류 코드가 없습니다."));
    }
}
