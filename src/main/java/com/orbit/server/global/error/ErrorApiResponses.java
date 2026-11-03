package com.orbit.server.global.error;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Arrays;
import java.util.Optional;

/**
 * 오류 코드로 API 문서의 오류 응답을 만든다.
 *
 * <p>오류 응답은 {@link GlobalExceptionHandler} 가 만들어 컨트롤러 시그니처에 드러나지 않으므로 문서에 따로 적어야 한다. 예시 본문을 오류 코드
 * enum 에서 바로 만들어, 코드와 메시지가 바뀌어도 문서가 실제 응답과 어긋나지 않게 한다.
 */
public final class ErrorApiResponses {

    /** API 문서에 등록하는 {@link ErrorResponse} 스키마 이름. */
    public static final String SCHEMA_NAME = "ErrorResponse";

    private static final String SCHEMA_REF = "#/components/schemas/" + SCHEMA_NAME;
    private static final String CODE_SEPARATOR = ", ";
    // GlobalExceptionHandler 가 오류 본문을 항상 JSON 으로 쓴다
    private static final String CONTENT_TYPE = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    private ErrorApiResponses() {}

    /**
     * 같은 HTTP 상태의 오류 코드들을 하나의 응답으로 묶어 operation 에 추가한다.
     *
     * <p>같은 상태의 오류 응답이 이미 있으면 예시를 합친다. API 별 오류와 공통 오류(예: 400 INVALID_INPUT)를 붙이는 순서와 관계없이 둘 다 남기기
     * 위해서다.
     *
     * @param operation 응답을 추가할 API
     * @param errorCodes 같은 HTTP 상태를 갖는 오류 코드
     * @throws InvalidValueException 오류 코드가 없거나 HTTP 상태가 서로 다른 경우
     */
    public static void addTo(Operation operation, ErrorCode... errorCodes) {
        String status = statusOf(errorCodes);
        responsesOf(operation).merge(status, response(errorCodes), ErrorApiResponses::merge);
    }

    private static String statusOf(ErrorCode... errorCodes) {
        long statusCount =
                Arrays.stream(errorCodes).map(ErrorCode::getStatus).distinct().count();
        if (statusCount != 1) {
            throw new InvalidValueException("한 응답에는 HTTP 상태가 같은 오류 코드만 묶을 수 있습니다.");
        }
        return String.valueOf(errorCodes[0].getStatus().value());
    }

    private static ApiResponses responsesOf(Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        return operation.getResponses();
    }

    private static ApiResponse response(ErrorCode... errorCodes) {
        MediaType mediaType = new MediaType().schema(new Schema<>().$ref(SCHEMA_REF));
        Arrays.stream(errorCodes).forEach(errorCode -> mediaType.addExamples(errorCode.getCode(), example(errorCode)));
        return new ApiResponse()
                .description(description(mediaType))
                .content(new Content().addMediaType(CONTENT_TYPE, mediaType));
    }

    // 기존 응답이 이 클래스가 만든 오류 응답이 아니면(예시가 없으면) 새 오류 응답으로 바꾼다
    private static ApiResponse merge(ApiResponse existing, ApiResponse added) {
        return errorMediaTypeOf(existing)
                .map(mediaType ->
                        addExamples(existing, mediaType, errorMediaTypeOf(added).orElseThrow()))
                .orElse(added);
    }

    private static ApiResponse addExamples(ApiResponse existing, MediaType target, MediaType source) {
        source.getExamples().forEach(target.getExamples()::putIfAbsent);
        return existing.description(description(target));
    }

    private static Optional<MediaType> errorMediaTypeOf(ApiResponse response) {
        return Optional.ofNullable(response.getContent())
                .map(content -> content.get(CONTENT_TYPE))
                .filter(mediaType -> mediaType.getExamples() != null);
    }

    private static String description(MediaType mediaType) {
        return String.join(CODE_SEPARATOR, mediaType.getExamples().keySet());
    }

    private static Example example(ErrorCode errorCode) {
        return new Example().summary(errorCode.getMessage()).value(ErrorResponse.of(errorCode));
    }
}
