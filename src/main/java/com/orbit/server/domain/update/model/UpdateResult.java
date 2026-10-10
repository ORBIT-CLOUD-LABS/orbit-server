package com.orbit.server.domain.update.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * 차량이 체크인으로 보고한 캠페인별 업데이트 결과.
 *
 * <p>차량은 응답을 못 받으면 같은 결과를 다시 보낸다. 차량·캠페인당 결과를 하나만 두고 덮어써서 멱등하게 처리한다.
 */
@Entity
@Table(name = "update_result")
public class UpdateResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Embedded
    private UpdateOutcome outcome;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "reported_at")
    private Instant reportedAt;

    protected UpdateResult() {}

    /**
     * 업데이트 결과를 기록한다.
     *
     * @param vehicleId 결과를 보고한 차량 ID. null 불가
     * @param campaignId 결과가 속한 캠페인 ID. null 불가
     * @param outcome 성공·실패와 실패 사유, 완료 시각. null 불가
     */
    public UpdateResult(Long vehicleId, Long campaignId, UpdateOutcome outcome) {
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicleId");
        this.campaignId = Objects.requireNonNull(campaignId, "campaignId");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
    }
}
