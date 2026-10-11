package com.orbit.server.domain.artifact.application;

import com.orbit.server.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Origin 파일 등록 중 발생하는 오류. (ORIGIN-REQ-001 / ORIGIN-POL-001)
 *
 * <p>HTTP 상태에 의존하므로 model 이 아닌 application 에 둔다.
 */
@Getter
public enum ArtifactErrorCode implements ErrorCode {
    ARTIFACT_VERSION_EXISTS(HttpStatus.CONFLICT, "같은 차종과 버전의 파일이 이미 등록되어 있습니다."),
    HASH_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "업로드한 파일의 SHA-256 이 요청한 값과 다릅니다."),
    // Origin 장애는 클라이언트 잘못이 아니므로 재시도할 수 있도록 502 로 구분한다
    ORIGIN_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "Origin 파일 서버에 업로드하지 못했습니다.");

    private final HttpStatus status;
    private final String message;

    ArtifactErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return name();
    }
}
