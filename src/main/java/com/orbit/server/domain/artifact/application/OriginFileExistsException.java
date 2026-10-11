package com.orbit.server.domain.artifact.application;

import com.orbit.server.global.error.BusinessException;

/**
 * Origin 경로에 이미 파일이 있어 업로드하지 않았을 때 던진다. 409 ARTIFACT_VERSION_EXISTS 로 응답한다.
 *
 * <p>같은 차종·버전 요청이 동시에 중복 확인을 통과해도 먼저 올린 한 요청만 성공하게 해, 저장된 해시와 Origin 파일이 어긋나지 않게 한다.
 * (ORIGIN-POL-001)
 */
public class OriginFileExistsException extends BusinessException {

    /**
     * @param cause Origin 이 업로드를 거부한 응답
     */
    public OriginFileExistsException(Throwable cause) {
        super(ArtifactErrorCode.ARTIFACT_VERSION_EXISTS, cause);
    }
}
