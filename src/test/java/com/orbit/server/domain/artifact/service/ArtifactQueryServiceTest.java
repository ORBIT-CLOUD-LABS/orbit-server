package com.orbit.server.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.artifact.exception.ArtifactErrorCode;
import com.orbit.server.domain.artifact.repository.ArtifactRepository;
import com.orbit.server.global.error.BusinessException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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

    private FakeArtifactRepository artifactRepository;
    private ArtifactQueryService service;

    @BeforeEach
    void setUp() {
        List<ArtifactSummary> summaries = List.of(MODEL_B, MODEL_A);
        artifactRepository = new FakeArtifactRepository(summaries);
        service = new ArtifactQueryService(new FakeArtifactSummaryQuery(summaries), artifactRepository);
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

    @Test
    @DisplayName("#18: 여러 ID 의 파일을 한 번에 조회하고 없는 ID 는 뺀다")
    void findsAllByIds() {
        assertThat(service.findAllByIds(List.of(1L, 3L))).containsExactly(MODEL_A);
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 파일 행을 잠근 뒤 조회한다")
    void locksAndGetsById() {
        assertThat(service.lockAndGetById(1L)).isEqualTo(MODEL_A);
        assertThat(artifactRepository.locked).containsExactly(1L);
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 잠글 파일이 없으면 ARTIFACT_NOT_FOUND 로 실패한다")
    void failsLockWhenArtifactNotFound() {
        assertThatThrownBy(() -> service.lockAndGetById(3L))
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

        @Override
        public List<ArtifactSummary> findAllByIds(Collection<Long> ids) {
            return summaries.stream()
                    .filter(summary -> ids.contains(summary.artifactId()))
                    .toList();
        }
    }

    private static class FakeArtifactRepository implements ArtifactRepository {

        private final List<ArtifactSummary> summaries;
        private final List<Long> locked = new ArrayList<>();

        FakeArtifactRepository(List<ArtifactSummary> summaries) {
            this.summaries = summaries;
        }

        @Override
        public Optional<Long> lockById(Long id) {
            Optional<Long> found = summaries.stream()
                    .map(ArtifactSummary::artifactId)
                    .filter(id::equals)
                    .findFirst();
            found.ifPresent(locked::add);
            return found;
        }

        // 조회 서비스는 등록 기능을 쓰지 않는다
        @Override
        public boolean existsByRelease(ArtifactRelease release) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Artifact save(Artifact artifact) {
            throw new UnsupportedOperationException();
        }
    }
}
