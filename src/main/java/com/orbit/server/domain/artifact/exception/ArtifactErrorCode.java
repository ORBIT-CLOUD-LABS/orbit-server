package com.orbit.server.domain.artifact.exception;

import com.orbit.server.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Origin 파일 등록·조회 중 발생하는 오류. (ORIGIN-REQ-001 / ORIGIN-POL-001, CAMPAIGN-REQ-001)
 *
 * <p>HTTP 상태에 의존하므로 Spring 에 의존하지 않는 entity 가 아닌 exception 에 둔다.
 */
@Getter
public enum ArtifactErrorCode implements ErrorCode {
    ARTIFACT_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 파일을 찾을 수 없습니다."),
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
