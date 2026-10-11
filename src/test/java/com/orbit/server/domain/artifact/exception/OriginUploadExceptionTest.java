package com.orbit.server.domain.artifact.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OriginUploadExceptionTest {

    @Test
    @DisplayName("ORIGIN-REQ-001: 업로드 실패는 ORIGIN_UPLOAD_FAILED 로 응답하고 원인 예외를 유지한다")
    void keepsErrorCodeAndCause() {
        IOException cause = new IOException("connection refused");

        OriginUploadException exception = new OriginUploadException(cause);

        assertThat(exception.getErrorCode()).isEqualTo(ArtifactErrorCode.ORIGIN_UPLOAD_FAILED);
        assertThat(exception).hasCause(cause);
    }
}
