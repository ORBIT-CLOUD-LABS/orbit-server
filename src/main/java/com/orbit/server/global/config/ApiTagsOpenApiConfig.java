package com.orbit.server.global.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 문서의 최상위 Tag 목록을 {@link ApiTags} 정의로 채운다.
 *
 * <p>springdoc 은 컨트롤러 {@code @Tag} 로 목록을 만들어 순서와 설명이 컨트롤러 곳곳에 흩어진다. 정의 한 곳의 순서·설명으로 바꿔 Swagger
 * UI 그룹 순서를 고정한다.
 */
@Configuration(proxyBeanMethods = false)
class ApiTagsOpenApiConfig {

    @Bean
    OpenApiCustomizer apiTagsCustomizer() {
        return openApi -> openApi.setTags(ApiTags.ordered());
    }
}
