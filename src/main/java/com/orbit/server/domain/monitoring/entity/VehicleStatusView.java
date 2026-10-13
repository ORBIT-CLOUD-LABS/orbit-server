package com.orbit.server.domain.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/**
 * 대시보드가 읽는 차량별 현재 상태와 최근 업데이트 결과. vehicle_status 뷰에 매핑한다. (MONITORING-REQ-004)
 *
 * <p>다른 컨텍스트의 테이블을 조인한 뷰라 쓰기를 막는다. 조회는 JPQL 생성자 표현식으로 바로 결과 형태로 읽으므로 getter 를 두지 않는다.
 * 결과와 실패 사유는 update 컨텍스트의 enum 을 참조하지 않도록 문자열로 둔다.
 */
@Entity
@Immutable
@Table(name = "vehicle_status")
public class VehicleStatusView {

    // 차량이 보내는 식별자. 차량당 한 행이라 ID 로 쓴다
    @Id
    @Column(name = "external_id")
    private String vehicleId;

    @Column(name = "model")
    private String model;

    @Column(name = "current_version")
    private String currentVersion;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "result")
    private String lastResult;

    @Column(name = "failure_reason")
    private String failureReason;

    protected VehicleStatusView() {}
}
