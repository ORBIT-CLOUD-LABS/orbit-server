package com.orbit.server.domain.update.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * 차량이 체크인으로 보고한 캠페인별 업데이트 결과.
 *
 * <p>차량은 응답을 못 받으면 같은 결과를 다시 보낸다. 차량·캠페인당 결과를 하나만 두고 덮어써서 멱등하게 처리한다. 동시 보고에도 UNIQUE 에
 * 걸리지 않도록 기록은 저장소의 upsert 쿼리로만 하므로 엔티티는 읽기 전용이다.
 */
@Entity
@Table(name = "update_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
}
