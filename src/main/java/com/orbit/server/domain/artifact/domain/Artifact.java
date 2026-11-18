package com.orbit.server.domain.artifact.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;

/**
 * Origin 에 등록한 업데이트 파일.
 *
 * <p>등록한 파일은 수정하지 않는다. 같은 차종의 새 버전은 새 Artifact 로 등록한다.
 */
@Entity
@Table(name = "artifact")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artifact {

    // 저장 전이면 null
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private ArtifactRelease release;

    @Embedded
    private ArtifactFile file;

    @Embedded
    private ArtifactSignature signature;

    // 등록 시각은 DB 시각(NOW(3))을 기준으로 한다
    @Generated
    @Column(name = "created_at")
    private Instant createdAt;

    /**
     * 등록 때 검증한 펌웨어 서명과 함께 업데이트 파일을 등록한다. (ORIGIN-POL-001)
     *
     * @param release 차종과 버전. null 불가
     * @param file 파일 해시·크기·Origin 경로. null 불가
     * @param signature 검증을 마친 펌웨어 서명과 키 버전. null 불가
     */
    public Artifact(ArtifactRelease release, ArtifactFile file, ArtifactSignature signature) {
        this.release = Objects.requireNonNull(release, "release");
        this.file = Objects.requireNonNull(file, "file");
        this.signature = Objects.requireNonNull(signature, "signature");
    }
}
