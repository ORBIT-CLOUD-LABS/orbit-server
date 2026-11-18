package com.orbit.server.domain.artifact.service;

import com.orbit.server.domain.artifact.entity.ArtifactSignature;
import com.orbit.server.global.error.InvalidValueException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 서버가 펌웨어 서명 검증에 쓰는 공개 키 목록. 키를 교체하는 동안에는 이전 키와 새 키를 함께 둔다. (ORIGIN-POL-001)
 *
 * <p>공개 키가 하나도 없거나 하나라도 잘못되면 모든 등록이 실패하거나 일부 키 버전의 서명만 조용히 거부되므로, 만들 때 모두 검증해 앱을 띄우지
 * 않는다.
 */
final class FirmwarePublicKeys {

    private final Map<String, FirmwarePublicKey> keysById;

    private FirmwarePublicKeys(Map<String, FirmwarePublicKey> keysById) {
        this.keysById = Map.copyOf(keysById);
    }

    /**
     * @param base64KeysById 키 버전({@code v1}, ...)별 공개 키(raw 32바이트의 base64)
     * @return 검증을 마친 공개 키 목록
     * @throws InvalidValueException 목록이 비었거나, 키 버전이 {@code v1} 형식이 아니거나, 공개 키가 잘못된 경우
     */
    static FirmwarePublicKeys fromBase64(Map<String, String> base64KeysById) {
        if (base64KeysById == null || base64KeysById.isEmpty()) {
            throw new InvalidValueException("펌웨어 서명 공개 키가 하나 이상 있어야 합니다.");
        }
        Map<String, FirmwarePublicKey> keysById = new HashMap<>();
        base64KeysById.forEach((keyId, base64) -> keysById.put(requireKeyId(keyId), toPublicKey(keyId, base64)));
        return new FirmwarePublicKeys(keysById);
    }

    /**
     * @param keyId 서명 키 버전
     * @return 이 버전의 공개 키. 모르는 버전이면 빈 값
     */
    Optional<FirmwarePublicKey> find(String keyId) {
        return Optional.ofNullable(keyId).map(keysById::get);
    }

    // 등록 요청의 키 버전은 v1 형식만 받으므로 형식이 다른 설정 키는 쓰이지 못한 채 남는다
    private static String requireKeyId(String keyId) {
        if (!ArtifactSignature.isKeyId(keyId)) {
            throw new InvalidValueException("펌웨어 서명 공개 키 버전은 v1, v2 같은 형식이어야 합니다: " + keyId);
        }
        return keyId;
    }

    // 어느 버전의 키가 잘못됐는지 알 수 있게 키 버전을 붙인다. 키 값은 붙이지 않는다
    private static FirmwarePublicKey toPublicKey(String keyId, String base64) {
        try {
            return FirmwarePublicKey.fromBase64(base64);
        } catch (InvalidValueException e) {
            throw new InvalidValueException(keyId + " 펌웨어 서명 공개 키가 잘못되었습니다. " + e.getMessage(), e);
        }
    }
}
