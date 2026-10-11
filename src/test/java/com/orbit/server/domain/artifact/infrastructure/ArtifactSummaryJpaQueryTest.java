package com.orbit.server.domain.artifact.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.artifact.application.ArtifactSummary;
import com.orbit.server.domain.artifact.application.ArtifactSummaryQuery;
import com.orbit.server.domain.artifact.model.Artifact;
import com.orbit.server.domain.artifact.model.ArtifactFile;
import com.orbit.server.domain.artifact.model.ArtifactRelease;
import com.orbit.server.domain.artifact.model.ArtifactRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ArtifactSummaryJpaQueryTest {

    private static final String SHA256 = "a".repeat(64);
    private static final long SIZE_BYTES = 1024;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private ArtifactSummaryQuery artifactSummaryQuery;

    @Test
    @DisplayName("ORIGIN-REQ-001: 전체 목록을 최근 등록한 순서로 조회한다")
    void findAllInLatestOrder() {
        Artifact first = save(new ArtifactRelease("model-a", "1.0.0"));
        Artifact second = save(new ArtifactRelease("model-b", "1.0.0"));

        List<ArtifactSummary> summaries = artifactSummaryQuery.findAll();

        assertThat(summaries).extracting(ArtifactSummary::artifactId).containsExactly(second.id(), first.id());
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 차종으로 필터링하면 그 차종의 파일만 최근 등록한 순서로 조회한다")
    void findAllByModelInLatestOrder() {
        Artifact older = save(new ArtifactRelease("model-a", "1.0.0"));
        save(new ArtifactRelease("model-b", "1.0.0"));
        Artifact newer = save(new ArtifactRelease("model-a", "1.0.1"));

        List<ArtifactSummary> summaries = artifactSummaryQuery.findAllByModel("model-a");

        assertThat(summaries).extracting(ArtifactSummary::artifactId).containsExactly(newer.id(), older.id());
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 조회 결과에 파일 정보와 등록 시각을 담는다")
    void summaryContainsFileInfo() {
        ArtifactRelease release = new ArtifactRelease("model-a", "1.0.0");
        Artifact artifact = save(release);

        ArtifactSummary summary = artifactSummaryQuery.findAll().getFirst();

        assertThat(summary)
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new ArtifactSummary(
                        artifact.id(), "model-a", "1.0.0", SIZE_BYTES, SHA256, release.originPath(), null));
        assertThat(summary.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 해당 차종의 파일이 없으면 빈 목록을 조회한다")
    void emptyWhenNoArtifactOfModel() {
        save(new ArtifactRelease("model-a", "1.0.0"));

        assertThat(artifactSummaryQuery.findAllByModel("model-b")).isEmpty();
    }

    private Artifact save(ArtifactRelease release) {
        return artifactRepository.save(
                new Artifact(release, new ArtifactFile(SHA256, SIZE_BYTES, release.originPath())));
    }
}
