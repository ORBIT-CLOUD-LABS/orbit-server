package com.orbit.server.domain.campaign.entity;

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

    /**
     * 응답과 다른 Aggregate 가 이 캠페인을 가리킬 때 쓰는 식별자. 상태를 노출하는 getter 가 아니므로 예외적으로 둔다.
     *
     * @return 저장 전이면 null
     */
    public Long id() {
        return id;
    }

    /**
     * 이 캠페인이 배포하는 Artifact 의 ID. 목표 버전은 Artifact 에 있으므로 대상 판정에서 이 ID 로 조회한다. 다른 Aggregate 를
     * 가리키는 식별자라 {@link #id()} 처럼 예외적으로 둔다.
     *
     * @return Artifact ID
     */
    public Long artifactId() {
        return artifactId;
    }

    /**
     * 차량이 이 캠페인의 대상 조건과 적용 기간을 모두 만족하는지 판정한다. (VEHICLE-POL-001)
     *
     * <p>활성 여부는 호출하는 쪽이 활성 캠페인만 골라 판정한다. 현재 버전이 이미 목표 버전인지는 목표 버전이 Artifact 에 있으므로
     * 여기서 보지 않고, 대상 캠페인을 고르는 쪽에서 거른다.
     *
     * @param vehicle 판정할 차량
     * @param now 판정 시각
     * @return 차종·HW·지역·현재 버전 범위·적용 기간을 모두 만족하면 true
     */
    public boolean targets(TargetVehicle vehicle, Instant now) {
        return condition.matches(vehicle, now);
    }

    /**
     * 같은 파일을 같은 대상 조건으로 배포하는 캠페인인지 판정한다. 적용 기간은 비교하지 않는다. (CAMPAIGN-POL-001)
     *
     * <p>조건이 일부만 겹치는 경우는 중복으로 보지 않는다. 한 차량이 여러 캠페인에 걸릴 때의 처리는 체크인 대상 판정에서 정한다.
     *
     * @param other 비교할 캠페인. 활성 여부는 호출하는 쪽이 활성 캠페인만 골라 넘긴다
     * @return 파일과 대상 조건이 모두 같으면 true
     */
    public boolean duplicates(Campaign other) {
        return artifactId.equals(other.artifactId) && condition.hasSameTargetAs(other.condition);
    }
}
