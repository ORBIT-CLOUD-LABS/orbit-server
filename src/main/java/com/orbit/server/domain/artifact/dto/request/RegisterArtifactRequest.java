package com.orbit.server.domain.artifact.dto.request;

import com.orbit.server.domain.artifact.domain.ArtifactRelease;
import com.orbit.server.domain.artifact.service.command.RegisterArtifactCommand;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * @param signature 펌웨어 서명 (Ed25519 64바이트의 base64). Vault 출력 {@code vault:v1:<base64>} 의 {@code <base64>} 부분. 필수
 * @param keyId 서명 키 버전 (예: {@code v1}). Vault 출력의 {@code v1} 부분. 필수
 */
public record RegisterArtifactRequest(
        @NotNull MultipartFile file,
        @NotBlank String model,
        @NotBlank String version,
        // 형식이 틀린 값은 해시 불일치(422)가 아니라 잘못된 요청(400)으로 구분한다
        @Pattern(regexp = "^[0-9a-fA-F]{64}$") String sha256,
        // 서명·키 버전은 필수지만 형식 검증을 걸지 않는다. 누락·형식 오류도 검증 실패와 같은 422 SIGNATURE_INVALID 로 응답해
        // 어떤 이유로 거부됐는지 구분해 주지 않기 위해서다. 대신 API 문서에는 필수로 표시해 클라이언트가 빠뜨리지 않게 한다
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String signature,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String keyId) {

    public RegisterArtifactCommand toCommand() {
        return new RegisterArtifactCommand(new ArtifactRelease(model, version), file, sha256, signature, keyId);
    }
}
