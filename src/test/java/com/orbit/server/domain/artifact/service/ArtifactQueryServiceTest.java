package com.orbit.server.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.domain.artifact.repository.ArtifactRepository;
import com.orbit.server.global.error.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ArtifactQueryServiceTest {

    private static final ArtifactSummary MODEL_A = summaryOf(1L, "model-a");
    private static final ArtifactSummary MODEL_B = summaryOf(2L, "model-b");

    private ArtifactRepository artifactRepository;
    private ArtifactQueryService service;

    @BeforeEach
    void setUp() {
        artifactRepository = mock(ArtifactRepository.class);
        service = new ArtifactQueryService(artifactRepository);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 차종을 주면 그 차종의 파일만 조회한다")
    void findsByModel() {
        given(artifactRepository.findSummariesByModel("model-a")).willReturn(List.of(MODEL_A));

        assertThat(service.findAll("model-a")).containsExactly(MODEL_A);
        verify(artifactRepository, never()).findAllSummaries();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("ORIGIN-REQ-001: 차종이 없거나 비어 있으면 전체 목록을 조회한다")
    void findsAllWithoutModel(String model) {
        given(artifactRepository.findAllSummaries()).willReturn(List.of(MODEL_B, MODEL_A));

        assertThat(service.findAll(model)).containsExactly(MODEL_B, MODEL_A);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: ID 로 파일을 조회한다")
    void getsById() {
        given(artifactRepository.findSummaryById(1L)).willReturn(Optional.of(MODEL_A));

        assertThat(service.getById(1L)).isEqualTo(MODEL_A);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 해당 ID 의 파일이 없으면 ARTIFACT_NOT_FOUND 로 실패한다")
    void failsWhenArtifactNotFound() {
        given(artifactRepository.findSummaryById(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(3L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ArtifactErrorCode.ARTIFACT_NOT_FOUND);
    }

    // 보조 API(제안)라 요구사항 ID 가 없다
    @Test
    @DisplayName("캠페인 목록: 여러 ID 의 파일을 한 번에 조회한다")
    void findsAllByIds() {
        given(artifactRepository.findSummariesByIds(List.of(1L, 3L))).willReturn(List.of(MODEL_A));

        assertThat(service.findAllByIds(List.of(1L, 3L))).containsExactly(MODEL_A);
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 파일 행을 잠근 뒤 조회한다")
    void locksAndGetsById() {
        given(artifactRepository.lockById(1L)).willReturn(Optional.of(1L));
        given(artifactRepository.findSummaryById(1L)).willReturn(Optional.of(MODEL_A));

        assertThat(service.lockAndGetById(1L)).isEqualTo(MODEL_A);
        verify(artifactRepository).lockById(1L);
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 잠글 파일이 없으면 ARTIFACT_NOT_FOUND 로 실패한다")
    void failsLockWhenArtifactNotFound() {
        given(artifactRepository.lockById(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.lockAndGetById(3L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ArtifactErrorCode.ARTIFACT_NOT_FOUND);
    }

    private static ArtifactSummary summaryOf(Long id, String model) {
        return new ArtifactSummary(id, model, "1.0.0", 1, "a".repeat(64), "/firmware/" + model, Instant.EPOCH);
    }
}
