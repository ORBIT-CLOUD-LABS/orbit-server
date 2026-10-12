package com.orbit.server.domain.vehicle.entity;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 차량 토큰 원문. 등록 응답에서 한 번만 내려주고 저장하지 않는다. 저장과 비교에는 {@link #hash()} 를 쓴다. (VEHICLE-REQ-002 /
 * VEHICLE-POL-003)
 *
 * @param value 토큰 원문. 공백 불가, 128자 이하
 */
public record VehicleToken(String value) {

    private static final String ALGORITHM = "SHA-256";
    // 256bit 난수. 추측으로 맞힐 수 없는 길이
    private static final int RANDOM_BYTES = 32;
    // 발급한 토큰은 43자다. 차량이 보낸 임의 길이의 값을 해시하지 않도록 상한을 둔다
    private static final int MAX_LENGTH = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * @throws InvalidValueException 토큰이 비어 있거나 128자를 넘는 경우
     */
    public VehicleToken {
        Preconditions.requireText(value, MAX_LENGTH, "차량 토큰");
    }

    /**
     * 새 토큰을 발급한다. 헤더에 그대로 넣을 수 있도록 패딩 없는 Base64URL 로 인코딩한다.
     *
     * @return 새 토큰
     */
    public static VehicleToken issue() {
        byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return new VehicleToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    /**
     * 저장·조회에 쓸 SHA-256 해시를 구한다. 토큰이 충분히 긴 난수라 솔트 없이 해시해도 원문을 추측할 수 없다.
     *
     * @return 소문자 16진수 SHA-256 해시
     */
    public TokenHash hash() {
        byte[] digest = newDigest().digest(value.getBytes(StandardCharsets.UTF_8));
        return new TokenHash(HexFormat.of().formatHex(digest));
    }

    // 로그·예외 메시지에 원문이 남지 않도록 감춘다
    @Override
    public String toString() {
        return "VehicleToken[****]";
    }

    // SHA-256 은 모든 Java 구현이 지원해야 하는 알고리즘이므로 실패는 실행 환경 문제다
    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " 을 지원하지 않는 실행 환경입니다.", e);
        }
    }
}
