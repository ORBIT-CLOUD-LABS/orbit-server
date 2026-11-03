package com.orbit.server.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Set;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;

class ApiErrorCodesCustomizerTest {

    private final ApiErrorCodesCustomizer customizer = new ApiErrorCodesCustomizer();

    @Test
    @DisplayName("같은 HTTP 상태의 오류 코드는 enum 타입이 달라도 하나의 응답으로 묶는다")
    void groupsErrorCodesByStatus() throws Exception {
        Operation operation = customizer.customize(new Operation(), handlerMethod("conflicts"));

        assertThat(operation.getResponses()).containsOnlyKeys("409", "422");
        assertThat(examplesOf(operation.getResponses().get("409")))
                .containsExactly(TestErrorCode.FIRST_CONFLICT.name(), OtherErrorCode.OTHER_CONFLICT.name());
        assertThat(operation.getResponses().get("409").getDescription()).isEqualTo("FIRST_CONFLICT, OTHER_CONFLICT");
    }

    @Test
    @DisplayName("어노테이션이 없으면 오류 응답을 추가하지 않는다")
    void ignoresMethodWithoutAnnotation() throws Exception {
        Operation operation = customizer.customize(new Operation(), handlerMethod("plain"));

        assertThat(operation.getResponses()).isNull();
    }

    @Test
    @DisplayName("enum 에 없는 오류 코드 이름이면 문서를 만들 때 실패한다")
    void rejectsUnknownCode() throws Exception {
        HandlerMethod handlerMethod = handlerMethod("unknownCode");

        assertThatThrownBy(() -> customizer.customize(new Operation(), handlerMethod))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("NO_SUCH_CODE");
    }

    private static HandlerMethod handlerMethod(String name) throws NoSuchMethodException {
        return new HandlerMethod(new TestController(), TestController.class.getDeclaredMethod(name));
    }

    private static Set<String> examplesOf(ApiResponse response) {
        return response.getContent().get("application/json").getExamples().keySet();
    }

    static class TestController {

        @ApiErrorCodes(
                type = TestErrorCode.class,
                codes = {"FIRST_CONFLICT", "UNPROCESSABLE"})
        @ApiErrorCodes(
                type = OtherErrorCode.class,
                codes = {"OTHER_CONFLICT"})
        void conflicts() {}

        void plain() {}

        @ApiErrorCodes(
                type = TestErrorCode.class,
                codes = {"NO_SUCH_CODE"})
        void unknownCode() {}
    }

    @Getter
    enum TestErrorCode implements ErrorCode {
        FIRST_CONFLICT(HttpStatus.CONFLICT, "첫 번째 충돌"),
        UNPROCESSABLE(HttpStatus.UNPROCESSABLE_CONTENT, "처리할 수 없음");

        private final HttpStatus status;
        private final String message;

        TestErrorCode(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        @Override
        public String getCode() {
            return name();
        }
    }

    @Getter
    enum OtherErrorCode implements ErrorCode {
        OTHER_CONFLICT(HttpStatus.CONFLICT, "다른 충돌");

        private final HttpStatus status;
        private final String message;

        OtherErrorCode(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        @Override
        public String getCode() {
            return name();
        }
    }
}
