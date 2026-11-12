package com.orbit.server.global.external.cdn;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 다운로드 URL 서명 설정. 비밀 값은 모든 Edge 와 함께 쓰는 값이라 배포 설정(Secret)으로 주입한다.
 *
 * <p>값이 없는 채로 뜨면 서명이 틀린 URL 을 내려줘 모든 다운로드가 403 이 되므로, 값이 없거나 형식이 맞지 않으면 앱을 띄우지 않는다.
 *
 * <p>서버는 서명만 하므로 지금 쓰는 키 한 쌍만 읽는다. 비밀 값을 바꿀 때는 Edge 에 새 키를 먼저 추가하고 이 설정을 새 키로 바꾼다. Edge 는
 * URL 의 {@code kid} 로 비밀 값을 고르므로 이전 키로 발급한 URL 도 만료까지 통한다.
 *
 * @param keyId 비밀 값의 버전. URL 의 {@code kid} 쿼리에 그대로 넣으므로 인코딩이 필요 없는 문자만 받는다. 환경 변수 {@code
 *     CDN_URL_SIGNING_KEY_ID} 로 넣는다
 * @param secret 서명 비밀 값. 짧으면 추측하기 쉬워 최소 길이를 둔다. 환경 변수 {@code CDN_URL_SIGNING_SECRET} 로 넣는다
 */
@Validated
@ConfigurationProperties("orbit.cdn.url-signing")
record UrlSigningProperties(
        @NotNull @Pattern(regexp = KEY_ID_PATTERN) String keyId,
        @NotNull @Size(min = MIN_SECRET_LENGTH) String secret) {

    private static final String KEY_ID_PATTERN = "[A-Za-z0-9_-]+";
    private static final int MIN_SECRET_LENGTH = 32;
}
