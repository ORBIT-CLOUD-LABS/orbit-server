package com.orbit.server.global.error;

import com.orbit.server.global.common.ApiResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 문서에 공통 오류 응답을 표시한다.
 *
 * <p>모든 API 에 500 {@code INTERNAL_SERVER_ERROR} 를, 요청 값(경로·쿼리 파라미터나 본문)을 받는 API 에 400 {@code INVALID_INPUT}
 * 을 붙인다. 인증 실패(401)는 인증 방식마다 코드가 달라 인증 스키마를 붙이는 쪽에서 표시한다.
 */
@Configuration(proxyBeanMethods = false)
class ErrorResponseOpenApiConfig {

    @Bean
    OpenApiCustomizer errorResponseCustomizer() {
        return openApi -> {
            registerSchema(openApi);
            openApi.getPaths().values().stream()
                    .map(PathItem::readOperations)
                    .flatMap(List::stream)
                    .forEach(ErrorResponseOpenApiConfig::addCommonErrors);
        };
    }

    private static void registerSchema(OpenAPI openApi) {
        Schema<?> schema =
                ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(ApiResponse.class)).schema;
        openApi.getComponents().addSchemas(ErrorApiResponses.SCHEMA_NAME, schema);
    }

    private static void addCommonErrors(Operation operation) {
        if (acceptsInput(operation)) {
            ErrorApiResponses.addTo(operation, CommonErrorCode.INVALID_INPUT);
        }
        ErrorApiResponses.addTo(operation, CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    private static boolean acceptsInput(Operation operation) {
        boolean hasParameters =
                operation.getParameters() != null && !operation.getParameters().isEmpty();
        return hasParameters || operation.getRequestBody() != null;
    }
}
