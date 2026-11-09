package com.orbit.server.domain.update.entity;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * 체크인마다 덮어쓰는 차량의 현재 상태. 대시보드 스냅샷과 SSE 변경분 조회의 출처다.
 *
 * <p>차량 하나에 상태 하나라 Vehicle ID 를 그대로 PK 로 쓴다.
 */
@Entity
@Table(name = "vehicle_state")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleState {

    // assigned_edge_id VARCHAR(50). Edge id 의 최대 길이(edge 컨텍스트 Edge)와 같다
    private static final int MAX_EDGE_ID_LENGTH = 50;

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

    // 매니페스트가 마지막으로 배정한 Edge. Edge 는 설정에만 있으므로 FK 없이 id 문자열로 둔다. 배정한 적 없으면 null
    @Column(name = "assigned_edge_id")
    private String assignedEdgeId;

    // SSE 가 이 시각으로 변경분을 조회하므로 DB 의 ON UPDATE 로만 갱신한다
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at")
    private Instant updatedAt;

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
     * 매니페스트가 고른 Edge 를 기록한다. 대시보드가 차량별 배정 Edge 를 보여 주는 데 쓴다.
     *
     * @param edgeId 고른 Edge 의 id. 공백 불가, 50자 이하
     * @throws InvalidValueException id 가 비어 있거나 50자를 넘는 경우
     */
    public void assignEdge(String edgeId) {
        this.assignedEdgeId = Preconditions.requireText(edgeId, MAX_EDGE_ID_LENGTH, "Edge id");
    }

    /**
     * Edge 선택에 쓸 수 있는 위치를 돌려준다. 차량이 움직이므로 너무 오래된 위치는 쓰지 않는다.
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
