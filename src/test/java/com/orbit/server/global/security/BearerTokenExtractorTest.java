package com.orbit.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class BearerTokenExtractorTest {

    private static final String TOKEN = "some-token";

    @Test
    @DisplayName("Bearer 형식이면 뒤의 값을 꺼낸다")
    void extractsBearerValue() {
        assertThat(BearerTokenExtractor.extract(requestWith("Bearer " + TOKEN))).contains(TOKEN);
    }

    @Test
    @DisplayName("Bearer 뒤가 비어 있으면 빈 문자열을 꺼낸다")
    void extractsEmptyValue() {
        assertThat(BearerTokenExtractor.extract(requestWith("Bearer "))).contains("");
    }

    @Test
    @DisplayName("값 안의 공백은 다듬지 않고 그대로 꺼낸다")
    void keepsValueAsIs() {
        assertThat(BearerTokenExtractor.extract(requestWith("Bearer  " + TOKEN + " ")))
                .contains(" " + TOKEN + " ");
    }

    @ParameterizedTest
    @ValueSource(strings = {TOKEN, "Basic " + TOKEN, "bearer " + TOKEN, "Bearer", ""})
    @DisplayName("Bearer 형식이 아니면 비어 있다")
    void returnsEmptyForNonBearer(String authorization) {
        assertThat(BearerTokenExtractor.extract(requestWith(authorization))).isEmpty();
    }

    @Test
    @DisplayName("헤더가 없으면 비어 있다")
    void returnsEmptyWithoutHeader() {
        assertThat(BearerTokenExtractor.extract(new MockHttpServletRequest())).isEmpty();
    }

    private static MockHttpServletRequest requestWith(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        return request;
    }
}
