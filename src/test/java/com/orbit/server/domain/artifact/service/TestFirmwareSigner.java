package com.orbit.server.domain.artifact.service;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;

/**
 * 테스트 전용 펌웨어 서명기. 운영에서는 Vault 가 서명하고 개인 키는 Vault 밖으로 나가지 않는다.
 *
 * <p>개인 키를 저장소에 남기지 않도록 만들 때마다 새 Ed25519 키 쌍을 생성한다.
 */
final class TestFirmwareSigner {

    private static final String ALGORITHM = "Ed25519";
    // JDK 공개 키의 X.509 인코딩(44바이트)에서 마지막 32바이트가 Vault 가 내보내는 것과 같은 raw 공개 키다
    private static final int RAW_KEY_LENGTH = 32;

    private final KeyPair keyPair;

    TestFirmwareSigner() {
        try {
            keyPair = KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return Vault 의 {@code public_key} 와 같은 형식(raw 32바이트의 base64)의 공개 키
     */
    String publicKeyBase64() {
        byte[] encoded = keyPair.getPublic().getEncoded();
        byte[] raw = Arrays.copyOfRange(encoded, encoded.length - RAW_KEY_LENGTH, encoded.length);
        return Base64.getEncoder().encodeToString(raw);
    }

    /**
     * @param message 서명 대상
     * @return Vault 출력 {@code vault:v1:<base64>} 의 {@code <base64>} 와 같은 형식의 서명
     */
    String sign(byte[] message) {
        try {
            Signature signer = Signature.getInstance(ALGORITHM);
            signer.initSign(keyPair.getPrivate());
            signer.update(message);
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
