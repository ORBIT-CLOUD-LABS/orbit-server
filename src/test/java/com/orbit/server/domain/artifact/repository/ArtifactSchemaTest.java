package com.orbit.server.domain.artifact.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactFile;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 엔티티가 아직 매핑하지 않은 [Phase2] 펌웨어 서명 컬럼의 제약을 SQL 로 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ArtifactSchemaTest {

    // Ed25519 서명 길이
    private static final String SIGNATURE = Base64.getEncoder().encodeToString(new byte[64]);

    @Autowired
    private EntityManager em;

    private Long artifactId;

    @BeforeEach
    void setUp() {
        ArtifactRelease release = new ArtifactRelease("model-a", "1.0.0");
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        em.persist(artifact);
        em.flush();
        artifactId = artifact.id();
    }

    @Test
    @DisplayName("서명 없이 등록한 파일도 저장할 수 있다 (등록 API 가 서명을 받기 전까지)")
    void allowsArtifactWithoutSignature() {
        assertThat(signature()).isEqualTo("null:null");
    }

    @Test
    @DisplayName("base64 서명과 키 버전을 저장한다")
    void storesSignature() {
        update(SIGNATURE, "v1");

        assertThat(signature()).isEqualTo(SIGNATURE + ":v1");
    }

    @Test
    @DisplayName("64바이트 서명의 base64 가 아니면 저장하지 않는다")
    void rejectsMalformedSignature() {
        String signatureOf32Bytes = Base64.getEncoder().encodeToString(new byte[32]);

        assertThatThrownBy(() -> update(signatureOf32Bytes, "v1")).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("Vault 키 버전 형식(v1, v2, ...)이 아니면 저장하지 않는다")
    void rejectsMalformedKeyId() {
        assertThatThrownBy(() -> update(SIGNATURE, "1")).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("키 버전은 대소문자를 구분한다 (V1 은 저장하지 않는다)")
    void rejectsUpperCaseKeyId() {
        assertThatThrownBy(() -> update(SIGNATURE, "V1")).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("서명과 키 버전 중 하나만 있으면 저장하지 않는다")
    void rejectsSignatureWithoutKeyId() {
        assertThatThrownBy(() -> update(SIGNATURE, null)).isInstanceOf(PersistenceException.class);
    }

    private void update(String signature, String keyId) {
        em.createNativeQuery("UPDATE artifact SET signature = :signature, key_id = :keyId WHERE id = :id")
                .setParameter("signature", signature)
                .setParameter("keyId", keyId)
                .setParameter("id", artifactId)
                .executeUpdate();
    }

    private String signature() {
        Object[] row = (Object[]) em.createNativeQuery("SELECT signature, key_id FROM artifact WHERE id = :id")
                .setParameter("id", artifactId)
                .getSingleResult();
        return row[0] + ":" + row[1];
    }
}
