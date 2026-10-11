package com.orbit.server.domain.artifact.exception;

import com.orbit.server.global.error.BusinessException;

/** Origin 업로드에 실패했을 때 던진다. 502 ORIGIN_UPLOAD_FAILED 로 응답한다. */
public class OriginUploadException extends BusinessException {

    /**
     * @param cause Origin 연동에서 발생한 원인 예외
     */
    public OriginUploadException(Throwable cause) {
        super(ArtifactErrorCode.ORIGIN_UPLOAD_FAILED, cause);
    }
}
