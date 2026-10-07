package com.orbit.server.domain.update.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * 체크인마다 덮어쓰는 차량의 현재 상태. 대시보드 스냅샷과 SSE 변경분 조회의 출처다.
 *
 * <p>차량 하나에 상태 하나라 Vehicle ID 를 그대로 PK 로 쓴다.
 */
@Entity
@Table(name = "vehicle_state")
public class VehicleState {

    @Id
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "current_version"))
    private SoftwareVersion currentVersion;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    // SSE 가 이 시각으로 변경분을 조회하므로 DB 의 ON UPDATE 로만 갱신한다
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected VehicleState() {}

    /**
     * 차량의 상태를 처음 기록한다.
     *
     * @param vehicleId 차량 ID. null 불가
     * @param currentVersion 차량의 현재 소프트웨어 버전. null 불가
     */
    public VehicleState(Long vehicleId, SoftwareVersion currentVersion) {
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicleId");
        this.currentVersion = Objects.requireNonNull(currentVersion, "currentVersion");
    }
}
