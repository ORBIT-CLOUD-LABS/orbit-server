package com.orbit.server.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorApiResponsesTest {

    private static final String JSON = "application/json";

    @Test
    @DisplayName("오류 코드로 상태별 응답과 실제 응답 본문 예시를 만든다")
    void addsErrorResponseWithExample() {
        Operation operation = new Operation();

        ErrorApiResponses.addTo(operation, CommonErrorCode.INVALID_INPUT);

        ApiResponse response = operation.getResponses().get("400");
        assertThat(response.getDescription()).isEqualTo("INVALID_INPUT");
        assertThat(response.getContent().get(JSON).getSchema().get$ref())
                .isEqualTo("#/components/schemas/" + ErrorApiResponses.SCHEMA_NAME);
        assertThat(response.getContent()
                        .get(JSON)
                        .getExamples()
                        .get("INVALID_INPUT")
                        .getValue())
                .isEqualTo(com.orbit.server.global.common.ApiResponse.failure(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("같은 상태의 오류 응답이 이미 있으면 예시를 합친다")
    void mergesExamplesOfSameStatus() {
        Operation operation = new Operation();

        ErrorApiResponses.addTo(operation, CommonErrorCode.CONFLICT);
        ErrorApiResponses.addTo(operation, CommonErrorCode.CONFLICT);
        ErrorApiResponses.addTo(operation, CommonErrorCode.INVALID_INPUT);
        ErrorApiResponses.addTo(operation, CommonErrorCode.INVALID_INPUT, CommonErrorCode.INVALID_INPUT);

        assertThat(operation.getResponses()).containsOnlyKeys("409", "400");
        assertThat(operation.getResponses().get("409").getContent().get(JSON).getExamples())
                .containsOnlyKeys("CONFLICT");
    }

    @Test
    @DisplayName("기존 응답이 오류 예시가 없는 응답이면 오류 응답으로 바꾼다")
    void replacesResponseWithoutErrorExamples() {
        Operation operation = new Operation()
                .responses(new ApiResponses().addApiResponse("404", new ApiResponse().content(new Content())));

        ErrorApiResponses.addTo(operation, CommonErrorCode.RESOURCE_NOT_FOUND);

        assertThat(operation.getResponses().get("404").getDescription()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("HTTP 상태가 다른 오류 코드는 한 응답으로 묶을 수 없다")
    void rejectsDifferentStatuses() {
        Operation operation = new Operation();

        assertThatThrownBy(() ->
                        ErrorApiResponses.addTo(operation, CommonErrorCode.INVALID_INPUT, CommonErrorCode.CONFLICT))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("오류 코드가 없으면 응답을 만들 수 없다")
    void rejectsEmptyErrorCodes() {
        Operation operation = new Operation();

        assertThatThrownBy(() -> ErrorApiResponses.addTo(operation)).isInstanceOf(InvalidValueException.class);
    }
}
