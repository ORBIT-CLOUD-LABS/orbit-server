package com.orbit.server.global.external.origin.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class OriginPropertiesTest {

    private static final String ENDPOINT = "orbit.origin.endpoint=";
    private static final String REGION = "orbit.origin.region=";
    private static final String[] REQUIRED_EXCEPT_REGION = {
        "orbit.origin.bucket=orbit-firmware", "orbit.origin.access-key=access", "orbit.origin.secret-key=secret"
    };

    // S3 클라이언트까지 만들면 바인딩 검증과 접속 방식 검증이 섞이므로 설정 객체만 등록한다
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesOnlyConfig.class)
            .withPropertyValues(REQUIRED_EXCEPT_REGION);

    @Test
    @DisplayName("endpoint 를 설정하면 S3 호환 서버 주소로 읽는다")
    void bindsCustomEndpoint() {
        contextRunner
                .withPropertyValues(ENDPOINT + "http://localhost:8333", REGION + "us-east-1")
                .run(context -> assertThat(
                                context.getBean(OriginProperties.class).customEndpoint())
                        .contains(URI.create("http://localhost:8333")));
    }

    @Test
    @DisplayName("endpoint 를 설정하지 않으면 AWS 기본 엔드포인트를 쓰도록 사용자 지정 주소가 없다")
    void hasNoCustomEndpointWhenOmitted() {
        contextRunner
                .withPropertyValues(REGION + "ap-northeast-2")
                .run(context -> assertThat(
                                context.getBean(OriginProperties.class).customEndpoint())
                        .isEmpty());
    }

    @Test
    @DisplayName("ORIGIN_ENDPOINT 를 빈 값으로 두면 설정하지 않은 것과 같게 본다")
    void hasNoCustomEndpointWhenBlank() {
        contextRunner
                .withPropertyValues(ENDPOINT, REGION + "ap-northeast-2")
                .run(context -> assertThat(
                                context.getBean(OriginProperties.class).customEndpoint())
                        .isEmpty());
    }

    @Test
    @DisplayName("리전이 없으면 AWS 의 다른 리전으로 요청하게 되므로 앱을 띄우지 않는다")
    void failsWithoutRegion() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("리전이 빈 값이면 앱을 띄우지 않는다")
    void failsWithBlankRegion(String region) {
        contextRunner
                .withPropertyValues(REGION + region)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OriginProperties.class)
    static class PropertiesOnlyConfig {}
}
