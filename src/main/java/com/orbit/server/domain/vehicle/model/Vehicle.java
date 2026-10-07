package com.orbit.server.domain.vehicle.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * OTA 서버에 등록한 차량.
 *
 * <p>체크인마다 바뀌는 현재 상태는 쓰기가 잦아 별도 Aggregate(VehicleState)로 분리한다. 여기에는 등록 시 갱신하는 정보만 둔다.
 */
@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private VehicleExternalId externalId;

    @Embedded
    private VehicleSpec spec;

    @Generated
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 수정 시각은 DB 의 ON UPDATE 로 갱신한다
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Vehicle() {}

    /**
     * 차량을 등록한다.
     *
     * @param externalId 차량이 보내는 식별자. null 불가
     * @param spec 차종·HW 버전·지역. null 불가
     */
    public Vehicle(VehicleExternalId externalId, VehicleSpec spec) {
        this.externalId = Objects.requireNonNull(externalId, "externalId");
        this.spec = Objects.requireNonNull(spec, "spec");
    }
}
