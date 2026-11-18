package com.orbit.server.domain.artifact.domain;

import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Base64;
import java.util.regex.Pattern;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 관리자가 Vault 로 만든 펌웨어 서명(Ed25519)과 서명 키 버전. 서버는 등록 때 검증한 값을 그대로 저장하고 매니페스트로 내려줘, 차량이 설치
 * 전에 다시 검증한다. (ORIGIN-REQ-001 / ORIGIN-POL-001)
 *
 * @param signature Ed25519 서명 64바이트의 base64. 패딩을 포함해 88자
 * @param keyId Vault Transit 키 버전 ({@code v1}, {@code v2}, ...). 차량은 이 값으로 내장 공개 키를 고른다
 */
@Embeddable
public record ArtifactSignature(
        @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "signature", length = 88)
        String signature,

        @Column(name = "key_id") String keyId) {

    // DB 검사 제약(ck_artifact_signature, ck_artifact_key_id)과 같은 형식
    private static final Pattern SIGNATURE_FORMAT = Pattern.compile("^[A-Za-z0-9+/]{86}==$");
    private static final Pattern KEY_ID_FORMAT = Pattern.compile("^v[1-9][0-9]*$");
    private static final int KEY_ID_MAX_LENGTH = 20;

    /**
     * @throws InvalidValueException 서명이 64바이트의 base64 가 아니거나, 키 버전이 {@code v1} 형식이 아닌 경우
     */
    public ArtifactSignature {
        requireSignature(signature);
        if (!isKeyId(keyId)) {
            throw new InvalidValueException("서명 키 버전은 v1, v2 같은 형식이어야 합니다.");
        }
    }

    /**
     * 서명 키 버전 형식인지 판정한다. 설정의 공개 키 버전도 이 형식이어야 등록 요청의 키 버전과 맞출 수 있다.
     *
     * @param keyId 판정할 값. null 가능
     * @return {@code v} 뒤에 0 으로 시작하지 않는 숫자가 오고 20자 이하면 true
     */
    public static boolean isKeyId(String keyId) {
        return keyId != null
                && keyId.length() <= KEY_ID_MAX_LENGTH
                && KEY_ID_FORMAT.matcher(keyId).matches();
    }

    /**
     * @return 서명 64바이트
     */
    public byte[] signatureBytes() {
        return Base64.getDecoder().decode(signature);
    }

    // 88자 base64 라도 마지막 글자의 남는 비트가 0 이 아니면 같은 서명이 여러 표기를 갖는다.
    // 차량의 디코더가 엄격하면 거부할 수 있으므로 표준 표기 하나만 받는다
    private static void requireSignature(String signature) {
        if (signature == null || !SIGNATURE_FORMAT.matcher(signature).matches()) {
            throw new InvalidValueException("서명은 64바이트의 base64 여야 합니다.");
        }
        byte[] decoded = Base64.getDecoder().decode(signature);
        if (!Base64.getEncoder().encodeToString(decoded).equals(signature)) {
            throw new InvalidValueException("서명은 표준 base64 표기여야 합니다.");
        }
    }
}
