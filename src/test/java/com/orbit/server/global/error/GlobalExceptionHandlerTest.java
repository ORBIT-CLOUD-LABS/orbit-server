package com.orbit.server.global.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    private static final String INTERNAL_MESSAGE = "internal detail: com.example.Secret";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("IllegalArgumentException 은 400 INVALID_INPUT 으로 응답한다")
    void respondsBadRequestForIllegalArgument() throws Exception {
        mockMvc.perform(get("/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.message").value(CommonErrorCode.INVALID_INPUT.getMessage()));
    }

    @Test
    @DisplayName("IllegalArgumentException 의 메시지는 응답에 노출하지 않는다")
    void hidesIllegalArgumentMessage() throws Exception {
        mockMvc.perform(get("/illegal-argument")).andExpect(content().string(not(containsString(INTERNAL_MESSAGE))));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/illegal-argument")
        void throwIllegalArgument() {
            throw new IllegalArgumentException(INTERNAL_MESSAGE);
        }
    }
}
