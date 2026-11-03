package com.orbit.server.global.error;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 가 응답할 수 있는 오류 코드를 문서에 표시한다.
 *
 * <p>오류 코드 enum 은 컨텍스트마다 타입이 달라 어노테이션 값으로 직접 받을 수 없으므로 enum 타입과 코드 이름을 받는다. 이름이 틀리면 API 문서를
 * 만들 때 실패하므로 문서 테스트에서 드러난다. 같은 HTTP 상태의 코드는 하나의 응답으로 묶여 표시된다. 공통 오류(400, 401, 500)는 따로 적지 않아도
 * 붙는다.
 *
 * <pre>{@code
 * @ApiErrorCodes(type = ArtifactErrorCode.class, codes = {"ARTIFACT_VERSION_EXISTS", "HASH_MISMATCH"})
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiErrorCodes.List.class)
public @interface ApiErrorCodes {

    /** 오류 코드 enum 타입. */
    Class<? extends ErrorCode> type();

    /** 이 API 가 응답할 수 있는 오류 코드 이름. */
    String[] codes();

    /** 여러 컨텍스트의 오류 코드를 함께 적기 위한 컨테이너. */
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {

        /** 반복해서 적은 {@link ApiErrorCodes}. */
        ApiErrorCodes[] value();
    }
}
