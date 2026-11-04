package com.orbit.server.domain.update.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
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
    private Instant lastSeenAt;

    // 위치와 갱신 시각은 체크인 쿼리가 DB 시계(NOW(3))로 함께 쓰므로 엔티티에서는 읽기만 한다
    @Embedded
    @AttributeOverride(name = "latitude", column = @Column(name = "latitude", insertable = false, updatable = false))
    @AttributeOverride(name = "longitude", column = @Column(name = "longitude", insertable = false, updatable = false))
    private VehicleLocation location;

    @Column(name = "location_updated_at", insertable = false, updatable = false)
    private Instant locationUpdatedAt;

    // SSE 가 이 시각으로 변경분을 조회하므로 DB 의 ON UPDATE 로만 갱신한다
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at")
    private Instant updatedAt;

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

    /**
     * 차량이 알려 준 현재 소프트웨어 버전으로 덮어쓴다.
     *
     * @param currentVersion 차량의 현재 소프트웨어 버전. null 불가
     */
    public void changeVersion(SoftwareVersion currentVersion) {
        this.currentVersion = Objects.requireNonNull(currentVersion, "currentVersion");
    }

    /**
     * Edge 선택에 쓸 수 있는 위치를 돌려준다. 차량이 움직이므로 너무 오래된 위치는 쓰지 않는다. (MANIFEST-POL-001)
     *
     * @param now 판단 기준 시각
     * @param maxAge 위치를 믿을 수 있는 최대 경과 시간
     * @return 받은 지 {@code maxAge} 이내인 마지막 위치. 받은 적이 없거나 더 오래됐으면 빈 값
     */
    public Optional<VehicleLocation> freshLocation(Instant now, Duration maxAge) {
        if (location == null || locationUpdatedAt.plus(maxAge).isBefore(now)) {
            return Optional.empty();
        }
        return Optional.of(location);
    }
}
