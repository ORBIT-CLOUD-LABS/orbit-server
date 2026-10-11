package com.orbit.server.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
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

    private ArtifactQueryService service;

    @BeforeEach
    void setUp() {
        service = new ArtifactQueryService(new FakeArtifactSummaryQuery(List.of(MODEL_B, MODEL_A)));
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 차종을 주면 그 차종의 파일만 조회한다")
    void findsByModel() {
        assertThat(service.findAll("model-a")).containsExactly(MODEL_A);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("ORIGIN-REQ-001: 차종이 없거나 비어 있으면 전체 목록을 조회한다")
    void findsAllWithoutModel(String model) {
        assertThat(service.findAll(model)).containsExactly(MODEL_B, MODEL_A);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: ID 로 파일을 조회한다")
    void getsById() {
        assertThat(service.getById(1L)).isEqualTo(MODEL_A);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 해당 ID 의 파일이 없으면 ARTIFACT_NOT_FOUND 로 실패한다")
    void failsWhenArtifactNotFound() {
        assertThatThrownBy(() -> service.getById(3L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ArtifactErrorCode.ARTIFACT_NOT_FOUND);
    }

    private static ArtifactSummary summaryOf(Long id, String model) {
        return new ArtifactSummary(id, model, "1.0.0", 1, "a".repeat(64), "/firmware/" + model, Instant.EPOCH);
    }

    private record FakeArtifactSummaryQuery(List<ArtifactSummary> summaries) implements ArtifactSummaryQuery {

        @Override
        public List<ArtifactSummary> findAll() {
            return summaries;
        }

        @Override
        public List<ArtifactSummary> findAllByModel(String model) {
            return summaries.stream()
                    .filter(summary -> summary.model().equals(model))
                    .toList();
        }

        @Override
        public Optional<ArtifactSummary> findById(Long id) {
            return summaries.stream()
                    .filter(summary -> summary.artifactId().equals(id))
                    .findFirst();
        }
    }
}
