package com.orbit.server.domain.campaign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 업데이트 파일을 조건에 맞는 차량에 배포하는 캠페인.
 *
 * <p>다른 Aggregate 인 Artifact 는 ID 로만 참조한다. 목표 버전은 Artifact 의 버전을 따르므로 따로 저장하지 않는다.
 */
@Entity
@Table(name = "campaign")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artifact_id")
    private Long artifactId;

    @Embedded
    private CampaignName name;

    @Embedded
    private CampaignCondition condition;

    // MySQL 방언은 enum 을 네이티브 ENUM 타입으로 매핑하므로 VARCHAR 컬럼에 맞춘다
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status")
    private CampaignStatus status;

    @Generated
    @Column(name = "created_at")
    private Instant createdAt;

    protected Campaign() {}

    /**
     * 캠페인을 등록한다. 등록 즉시 활성 상태가 되어 체크인 대상 판정에 반영된다.
     *
     * @param name 캠페인 이름. null 불가
     * @param artifactId 배포할 Artifact ID. null 불가
     * @param condition 대상 차량 조건과 적용 기간. null 불가
     */
    public Campaign(CampaignName name, Long artifactId, CampaignCondition condition) {
        this.name = Objects.requireNonNull(name, "name");
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
        this.condition = Objects.requireNonNull(condition, "condition");
        this.status = CampaignStatus.ACTIVE;
    }
}
