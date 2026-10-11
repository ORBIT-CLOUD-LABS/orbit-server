package com.orbit.server.domain.artifact.presentation;

import com.orbit.server.domain.artifact.application.ArtifactRegistrationResult;
import com.orbit.server.domain.artifact.application.ArtifactRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자용 Origin 파일 API. */
@RestController
@RequestMapping("/api/v1/admin/artifacts")
@RequiredArgsConstructor
public class ArtifactAdminController {

    private final ArtifactRegistrationService artifactRegistrationService;

    /**
     * 업데이트 파일을 Origin 에 올리고 Artifact 로 등록한다. (ORIGIN-REQ-001 / ORIGIN-POL-001)
     *
     * @param request 파일, 차종, 버전, (선택) SHA-256
     * @return 201 과 등록한 Artifact 의 ID·크기·해시·경로
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArtifactRegistrationResponse> register(
            @Valid @ModelAttribute RegisterArtifactRequest request) {
        ArtifactRegistrationResult result = artifactRegistrationService.register(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ArtifactRegistrationResponse.from(result));
    }
}
