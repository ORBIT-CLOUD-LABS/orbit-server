package com.orbit.server.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("도메인 값 검증 실패는 400 INVALID_INPUT 으로 응답한다")
    void respondsBadRequestForInvalidValue() throws Exception {
        mockMvc.perform(get("/invalid-value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));
    }

    @Test
    @DisplayName("IllegalArgumentException 은 서버 오류가 가려지지 않도록 500 으로 응답한다")
    void respondsInternalServerErrorForIllegalArgument() throws Exception {
        mockMvc.perform(get("/illegal-argument"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/invalid-value")
        void throwInvalidValue() {
            throw new InvalidValueException("값이 올바르지 않습니다.");
        }

        @GetMapping("/illegal-argument")
        void throwIllegalArgument() {
            throw new IllegalArgumentException("서버 내부 오류");
        }
    }
}
