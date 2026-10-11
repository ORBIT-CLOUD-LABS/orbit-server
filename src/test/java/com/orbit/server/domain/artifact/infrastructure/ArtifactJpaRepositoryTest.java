package com.orbit.server.domain.artifact.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.artifact.model.Artifact;
import com.orbit.server.domain.artifact.model.ArtifactFile;
import com.orbit.server.domain.artifact.model.ArtifactRelease;
import com.orbit.server.domain.artifact.model.ArtifactRepository;
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
class ArtifactJpaRepositoryTest {

    private static final ArtifactRelease RELEASE = new ArtifactRelease("model-a", "1.0.0");

    @Autowired
    private ArtifactRepository artifactRepository;

    @Test
    @DisplayName("ORIGIN-POL-001: 같은 차종·버전이 저장되어 있으면 이미 있다고 판단한다")
    void existsWhenSameReleaseSaved() {
        artifactRepository.save(artifactOf(RELEASE));

        assertThat(artifactRepository.existsByRelease(RELEASE)).isTrue();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 같은 차종의 다른 버전만 있으면 없다고 판단한다")
    void doesNotExistForOtherVersion() {
        artifactRepository.save(artifactOf(RELEASE));

        assertThat(artifactRepository.existsByRelease(new ArtifactRelease("model-a", "1.0.1")))
                .isFalse();
    }

    private Artifact artifactOf(ArtifactRelease release) {
        return new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
    }
}
