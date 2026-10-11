package com.orbit.server.domain.artifact.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.domain.artifact.exception.OriginUploadException;
import com.orbit.server.domain.artifact.service.ArtifactQueryService;
import com.orbit.server.domain.artifact.service.ArtifactRegistrationResult;
import com.orbit.server.domain.artifact.service.ArtifactRegistrationService;
import com.orbit.server.domain.artifact.service.ArtifactSummary;
import com.orbit.server.domain.artifact.service.RegisterArtifactCommand;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.CommonErrorCode;
import com.orbit.server.global.error.GlobalExceptionHandler;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ArtifactAdminControllerTest {

    private static final String URL = "/api/v1/admin/artifacts";
    private static final String SHA256 = "a".repeat(64);
    private static final String PATH = "/firmware/model-a/1.0.0/firmware.bin";
    private static final String CREATED_AT = "2026-10-11T07:00:00.123Z";
    private static final MockMultipartFile FILE = new MockMultipartFile("file", "firmware.bin", null, new byte[] {1});

    private ArtifactRegistrationService artifactRegistrationService;
    private ArtifactQueryService artifactQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        artifactRegistrationService = mock(ArtifactRegistrationService.class);
        artifactQueryService = mock(ArtifactQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ArtifactAdminController(artifactRegistrationService, artifactQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 차종으로 필터링한 파일 목록을 200 으로 응답한다")
    void respondsArtifactsOfModel() throws Exception {
        given(artifactQueryService.findAll("model-a"))
                .willReturn(List.of(
                        new ArtifactSummary(1L, "model-a", "1.0.0", 1, SHA256, PATH, Instant.parse(CREATED_AT))));

        mockMvc.perform(get(URL).param("model", "model-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts.length()").value(1))
                .andExpect(jsonPath("$.artifacts[0].artifactId").value(1))
                .andExpect(jsonPath("$.artifacts[0].model").value("model-a"))
                .andExpect(jsonPath("$.artifacts[0].version").value("1.0.0"))
                .andExpect(jsonPath("$.artifacts[0].fileSize").value(1))
                .andExpect(jsonPath("$.artifacts[0].sha256").value(SHA256))
                .andExpect(jsonPath("$.artifacts[0].path").value(PATH))
                .andExpect(jsonPath("$.artifacts[0].createdAt").value(CREATED_AT));
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 차종 없이 요청하면 전체 목록을 조회한다")
    void findsAllWithoutModel() throws Exception {
        given(artifactQueryService.findAll(null)).willReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts").isEmpty());
        verify(artifactQueryService).findAll(null);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 파일을 등록하면 201 과 ID·크기·해시·경로를 응답한다")
    void respondsCreated() throws Exception {
        given(artifactRegistrationService.register(any()))
                .willReturn(new ArtifactRegistrationResult(1L, 1, SHA256, PATH));

        mockMvc.perform(validRequest().param("sha256", SHA256))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.artifactId").value(1))
                .andExpect(jsonPath("$.fileSize").value(1))
                .andExpect(jsonPath("$.sha256").value(SHA256))
                .andExpect(jsonPath("$.path").value(PATH));

        ArgumentCaptor<RegisterArtifactCommand> command = ArgumentCaptor.forClass(RegisterArtifactCommand.class);
        verify(artifactRegistrationService).register(command.capture());
        assertThat(command.getValue().release()).isEqualTo(new ArtifactRelease("model-a", "1.0.0"));
        assertThat(command.getValue().findExpectedSha256()).contains(SHA256);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 파일이 없으면 400 INVALID_INPUT 으로 응답한다")
    void rejectsMissingFile() throws Exception {
        mockMvc.perform(multipart(URL).param("model", "model-a").param("version", "1.0.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));
        verify(artifactRegistrationService, never()).register(any());
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: SHA-256 형식이 틀리면 해시 불일치가 아닌 400 INVALID_INPUT 으로 응답한다")
    void rejectsMalformedSha256() throws Exception {
        mockMvc.perform(validRequest().param("sha256", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()));
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 해시가 다르면 422 HASH_MISMATCH 로 응답한다")
    void respondsHashMismatch() throws Exception {
        given(artifactRegistrationService.register(any()))
                .willThrow(new BusinessException(ArtifactErrorCode.HASH_MISMATCH));

        mockMvc.perform(validRequest())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value(ArtifactErrorCode.HASH_MISMATCH.getCode()));
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 같은 차종·버전이 있으면 409 ARTIFACT_VERSION_EXISTS 로 응답한다")
    void respondsVersionExists() throws Exception {
        given(artifactRegistrationService.register(any()))
                .willThrow(new BusinessException(ArtifactErrorCode.ARTIFACT_VERSION_EXISTS));

        mockMvc.perform(validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ArtifactErrorCode.ARTIFACT_VERSION_EXISTS.getCode()));
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: Origin 업로드에 실패하면 502 ORIGIN_UPLOAD_FAILED 로 응답한다")
    void respondsOriginUploadFailed() throws Exception {
        given(artifactRegistrationService.register(any()))
                .willThrow(new OriginUploadException(new IOException("origin unavailable")));

        mockMvc.perform(validRequest())
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(ArtifactErrorCode.ORIGIN_UPLOAD_FAILED.getCode()));
    }

    private MockMultipartHttpServletRequestBuilder validRequest() {
        return multipart(URL).file(FILE).param("model", "model-a").param("version", "1.0.0");
    }
}
