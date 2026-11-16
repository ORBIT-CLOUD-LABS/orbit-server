package com.orbit.server.domain.artifact.service;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 펌웨어 서명 공개 키 설정. 공개 키는 비밀이 아니므로 배포 설정(ConfigMap)으로 넣는다. (ORIGIN-POL-001)
 *
 * <p>서버는 서명하지 않고 등록 때 검증만 하므로 공개 키만 읽는다. 키를 교체하는 동안에는 이전 버전과 새 버전을 함께 둔다.
 *
 * <p>값이 없거나 잘못된 채로 뜨면 모든 등록이 {@code SIGNATURE_INVALID} 로 실패하므로 앱을 띄우지 않는다. 검증 규칙은 {@link
 * FirmwarePublicKeys} 가 맡는다.
 *
 * @param publicKeys 키 버전({@code v1}, ...)별 공개 키. Vault {@code transit/keys/firmware-signing} 의 {@code public_key}(raw
 *     32바이트의 base64). 키 버전이 정해져 있지 않아 환경 변수 자리표시자 대신 {@code ORBIT_SIGNING_PUBLICKEYS_V1} 같은 환경 변수나 설정
 *     파일로 넣는다
 */
@ConfigurationProperties("orbit.signing")
record SigningProperties(
        // 설정이 빠지면 null 이 아니라 빈 맵으로 받아, 비어 있다는 같은 이유로 FirmwarePublicKeys 가 거부하게 한다
        @DefaultValue Map<String, String> publicKeys) {

    /**
     * @return 검증을 마친 공개 키 목록
     * @throws com.orbit.server.global.error.InvalidValueException 공개 키가 없거나 키 버전·공개 키가 잘못된 경우
     */
    FirmwarePublicKeys toPublicKeys() {
        return FirmwarePublicKeys.fromBase64(publicKeys);
    }
}
