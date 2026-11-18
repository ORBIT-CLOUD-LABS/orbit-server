package com.orbit.server.domain.artifact.service;

import com.orbit.server.domain.artifact.entity.ArtifactSignature;
import com.orbit.server.domain.artifact.entity.FirmwareSignaturePayload;
import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.InvalidValueException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 관리자가 등록 요청에 담은 펌웨어 서명을 설정의 공개 키로 검증한다. (ORIGIN-REQ-001 / ORIGIN-POL-001)
 *
 * <p>잘못된 서명이 등록되지 않게 미리 거르는 용도다. 서버가 장악되면 건너뛸 수 있으므로 최종 방어선은 설치 전 차량의 검증이다.
 *
 * <p>실패 원인은 응답으로 구분하지 않고 로그에만 남긴다. 로그에는 원인 종류만 남긴다. 요청 값(서명·키 버전·차종·버전) 원문은 요청한 쪽이 마음대로
 * 넣을 수 있어 로그를 위조하는 데 쓰일 수 있으므로 남기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FirmwareSignatureVerifier {

    private final FirmwarePublicKeys publicKeys;

    /**
     * @param payload 서명 대상. 서버가 계산한 해시·크기로 만든다
     * @param signature 요청의 서명 (64바이트의 base64). 없으면 null
     * @param keyId 요청의 서명 키 버전. 없으면 null
     * @return 검증을 통과한 서명. 그대로 저장해 매니페스트로 내려준다
     * @throws BusinessException 서명·키 버전이 없거나 형식이 틀리거나, 모르는 키 버전이거나, 서명이 맞지 않으면 SIGNATURE_INVALID
     */
    ArtifactSignature verify(FirmwareSignaturePayload payload, String signature, String keyId) {
        ArtifactSignature parsed = parse(signature, keyId);
        FirmwarePublicKey publicKey =
                publicKeys.find(parsed.keyId()).orElseThrow(() -> rejected(Reason.UNKNOWN_KEY_ID));
        if (!publicKey.verifies(payload.toBytes(), parsed.signatureBytes())) {
            throw rejected(Reason.MISMATCH);
        }
        return parsed;
    }

    private ArtifactSignature parse(String signature, String keyId) {
        try {
            return new ArtifactSignature(signature, keyId);
        } catch (InvalidValueException e) {
            log.warn("펌웨어 서명 검증 실패: reason={}", Reason.MISSING_OR_MALFORMED);
            throw new BusinessException(ArtifactErrorCode.SIGNATURE_INVALID, e);
        }
    }

    private BusinessException rejected(Reason reason) {
        log.warn("펌웨어 서명 검증 실패: reason={}", reason);
        return new BusinessException(ArtifactErrorCode.SIGNATURE_INVALID);
    }

    // 응답으로는 구분하지 않는 실패 원인. 운영자가 로그로 원인을 찾을 수 있게 남긴다
    private enum Reason {
        MISSING_OR_MALFORMED,
        UNKNOWN_KEY_ID,
        MISMATCH
    }
}
