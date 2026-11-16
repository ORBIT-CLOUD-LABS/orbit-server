package com.orbit.server.domain.artifact.service;

import com.orbit.server.global.error.InvalidValueException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 펌웨어 서명을 검증하는 Ed25519 공개 키. Vault Transit 이 내보내는 공개 키(RFC 8032 인코딩 32바이트의 base64)로 만든다.
 * (ORIGIN-POL-001)
 *
 * <p>외부 라이브러리 없이 JDK 의 Ed25519 구현을 쓴다. JDK 는 raw 32바이트를 바로 받지 않으므로 RFC 8410 의 SubjectPublicKeyInfo 로 감싸
 * 넘기고, 32바이트를 점으로 해석하는 일은 JDK 에 맡긴다.
 */
final class FirmwarePublicKey {

    private static final String ALGORITHM = "Ed25519";
    private static final int KEY_LENGTH = 32;
    // RFC 8410 의 Ed25519 SubjectPublicKeyInfo 에서 공개 키 32바이트 앞에 오는 DER 고정 부분 (알고리즘 OID 1.3.101.112)
    private static final byte[] X509_PREFIX = HexFormat.of().parseHex("302a300506032b6570032100");

    private final PublicKey key;

    private FirmwarePublicKey(PublicKey key) {
        this.key = key;
    }

    /**
     * @param base64 RFC 8032 인코딩 공개 키 32바이트의 base64
     * @return 검증에 쓸 공개 키
     * @throws InvalidValueException base64 가 아니거나, 32바이트가 아니거나, Ed25519 곡선 위의 점이 아닌 경우. 키 값은 메시지에 넣지 않는다
     */
    static FirmwarePublicKey fromBase64(String base64) {
        byte[] raw = decode(base64);
        if (raw.length != KEY_LENGTH) {
            throw new InvalidValueException("공개 키는 " + KEY_LENGTH + "바이트여야 합니다.");
        }
        return new FirmwarePublicKey(toPublicKey(raw));
    }

    /**
     * 서명이 이 공개 키로 메시지에 대해 만든 것인지 확인한다.
     *
     * @param message 서명 대상 바이트열
     * @param signature 서명 바이트열
     * @return 서명이 맞으면 true. 길이가 틀리거나 표준 형식이 아닌 서명도 false
     */
    boolean verifies(byte[] message, byte[] signature) {
        try {
            Signature verifier = newVerifier(key);
            verifier.update(message);
            return verifier.verify(signature);
        } catch (SignatureException e) {
            // JDK 는 길이가 틀리거나 S 가 범위를 넘는 서명에 false 대신 예외를 던진다. 위조된 서명과 같게 본다
            return false;
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("만들 때 확인한 공개 키로 검증을 시작하지 못했습니다.", e);
        }
    }

    private static byte[] decode(String base64) {
        if (base64 == null) {
            throw new InvalidValueException("공개 키가 없습니다.");
        }
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("공개 키는 base64 여야 합니다.", e);
        }
    }

    // KeyFactory 는 점이 곡선 위에 있는지 보지 않으므로, 검증을 시작해 봐서 잘못된 키를 기동 때 드러낸다
    private static PublicKey toPublicKey(byte[] raw) {
        byte[] encoded = new byte[X509_PREFIX.length + raw.length];
        System.arraycopy(X509_PREFIX, 0, encoded, 0, X509_PREFIX.length);
        System.arraycopy(raw, 0, encoded, X509_PREFIX.length, raw.length);
        try {
            PublicKey key = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(encoded));
            newVerifier(key);
            return key;
        } catch (InvalidKeyException e) {
            throw new InvalidValueException("공개 키가 Ed25519 곡선 위의 점이 아닙니다.", e);
        } catch (GeneralSecurityException e) {
            throw new InvalidValueException("공개 키를 Ed25519 키로 읽지 못했습니다.", e);
        }
    }

    // Signature 는 스레드 안전하지 않으므로 검증마다 새로 만든다
    private static Signature newVerifier(PublicKey key) throws InvalidKeyException {
        Signature verifier = newSignature();
        verifier.initVerify(key);
        return verifier;
    }

    // Ed25519 는 Java 15 부터 모든 구현이 지원해야 하는 알고리즘이므로 실패는 실행 환경 문제다
    private static Signature newSignature() {
        try {
            return Signature.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " 을 지원하지 않는 실행 환경입니다.", e);
        }
    }
}
