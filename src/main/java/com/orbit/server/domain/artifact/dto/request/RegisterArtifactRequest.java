package com.orbit.server.domain.artifact.dto.request;

import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.artifact.service.RegisterArtifactCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

/**
 * Origin 파일 등록 요청. {@code multipart/form-data} 로 받는다.
 *
 * @param file 업데이트 파일
 * @param model 차종
 * @param version 소프트웨어 버전
 * @param sha256 관리자가 계산한 SHA-256. 선택이며, 보내면 서버가 계산한 값과 비교한다
 */
public record RegisterArtifactRequest(
        @NotNull MultipartFile file,
        @NotBlank String model,
        @NotBlank String version,
        // 형식이 틀린 값은 해시 불일치(422)가 아니라 잘못된 요청(400)으로 구분한다
        @Pattern(regexp = "^[0-9a-fA-F]{64}$") String sha256) {

    public RegisterArtifactCommand toCommand() {
        return new RegisterArtifactCommand(new ArtifactRelease(model, version), file, sha256);
    }
}
