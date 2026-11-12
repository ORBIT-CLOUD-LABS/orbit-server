package com.orbit.server.global.external.cdn;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * CDN Edge 의 nginx {@code secure_link} 가 검증하는 다운로드 URL 서명을 만든다.
 *
 * <p>Edge 는 {@code secure_link_md5 "$secure_link_expires$uri $cdn_secret"} 로 같은 값을 다시 계산해 비교하므로, 형식이 한 글자라도 다르면
 * 모든 다운로드가 403 이 된다. 서명에 host 를 넣지 않아 같은 서명이 모든 Edge 에서 통한다.
 *
 * <p>MD5 는 nginx 기본 {@code secure_link} 모듈이 지원하는 유일한 방식이라 쓴다. 알고리즘을 바꿀 때 이 클래스만 고치면 되도록 서명 계산을
 * 여기에 모은다.
 */
final class SecureLinkSigner {

    private static final String ALGORITHM = "MD5";
    private static final String SECRET_SEPARATOR = " ";

    private final String secret;

    /**
     * @param secret 모든 Edge 와 함께 쓰는 서명 비밀 값
     */
    SecureLinkSigner(String secret) {
        this.secret = secret;
    }

    /**
     * 만료 시각과 경로에 대한 서명을 만든다.
     *
     * @param path Origin 경로. nginx {@code $uri} 가 디코딩된 경로이므로 URL 인코딩하지 않은 값을 넘긴다
     * @param expires URL 만료 시각(Unix 초). URL 의 {@code expires} 쿼리와 한 글자라도 다르면 서명이 맞지 않으므로, 쿼리에 넣는 바로 그
     *     값을 받는다
     * @return 쿼리 {@code md5} 에 그대로 넣을 수 있는 base64url 서명(패딩 없음)
     */
    String sign(String path, long expires) {
        String signed = expires + path + SECRET_SEPARATOR + secret;
        byte[] digest = newDigest().digest(signed.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " 을 지원하지 않는 실행 환경입니다.", e);
        }
    }
}
