package com.orbit.server.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.global.error.CommonErrorCode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ApiResponseTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("성공 응답은 OK 코드와 본문을 담는다")
    void createsSuccess() {
        ApiResponse<String> response = ApiResponse.success("본문");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.message()).isEqualTo(SuccessCode.OK.getMessage());
        assertThat(response.data()).isEqualTo("본문");
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("지정한 성공 코드로 응답한다")
    void createsSuccessWithCode() {
        ApiResponse<String> response = ApiResponse.success(SuccessCode.CREATED, "본문");

        assertThat(response.code()).isEqualTo("CREATED");
        assertThat(response.message()).isEqualTo(SuccessCode.CREATED.getMessage());
    }

    @Test
    @DisplayName("실패 응답은 오류 코드와 메시지를 담고 본문은 비운다")
    void createsFailure() {
        ApiResponse<Void> response = ApiResponse.failure(CommonErrorCode.INVALID_INPUT);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.code()).isEqualTo(CommonErrorCode.INVALID_INPUT.getCode());
        assertThat(response.message()).isEqualTo(CommonErrorCode.INVALID_INPUT.getMessage());
        assertThat(response.data()).isNull();
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("JSON 필드 이름은 isSuccess 이고 정해진 순서로, null 필드도 빠짐없이 나간다")
    void serializesInDefinedShape() {
        String json = jsonMapper.writeValueAsString(ApiResponse.success(Map.of("userId", 1)));

        assertThat(json)
                .isEqualTo("{\"isSuccess\":true,\"code\":\"OK\",\"message\":\"요청에 성공했습니다.\","
                        + "\"data\":{\"userId\":1},\"error\":null}");
    }
}
