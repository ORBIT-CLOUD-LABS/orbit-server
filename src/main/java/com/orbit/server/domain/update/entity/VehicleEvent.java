package com.orbit.server.domain.update.entity;

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
 * 차량의 업데이트 시도 한 번의 이력.
 *
 * <p>{@link UpdateResult} 는 차량·캠페인당 결과 하나를 덮어써서 실패 후 재시도한 기록이 사라진다. 어떤 버전이 언제 실패·성공했는지 보여 주려고
 * 시도마다 한 행씩 쌓는다. 기록은 저장소의 기록 쿼리로만 하므로 엔티티는 읽기 전용이다.
 */
@Entity
@Table(name = "vehicle_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Embedded
    private UpdateOutcome outcome;

    @Generated(event = EventType.INSERT)
    @Column(name = "recorded_at")
    private Instant recordedAt;
}
