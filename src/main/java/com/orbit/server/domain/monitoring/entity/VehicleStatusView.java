package com.orbit.server.domain.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 대시보드가 읽는 차량별 현재 상태와 최근 업데이트 결과. vehicle_status 뷰에 매핑한다. (MONITORING-REQ-004)
 *
 * <p>스냅샷·SSE 는 목록에 필요한 컬럼만 읽고, 차량 상세는 HW 버전·지역·실패 상세·[Phase2] 위치·Edge 까지 읽는다.
 *
 * <p>다른 컨텍스트의 테이블을 조인한 뷰라 쓰기를 막는다. 조회는 필요한 컬럼만 JPQL 생성자 표현식으로 바로 결과 형태로 읽는다.
 * 결과와 실패 사유는 update 컨텍스트의 enum 을 참조하지 않도록 문자열로 둔다.
 */
@Entity
@Immutable
@Table(name = "vehicle_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "hw_version")
    private String hwVersion;

    @Column(name = "region")
    private String region;

    @Column(name = "failure_detail")
    private String failureDetail;

    @Column(name = "finished_at")
    private Instant finishedAt;

    // 위치를 받은 적 없으면 셋 다 null 이라 원시 타입 대신 래퍼 타입을 쓴다.
    // 컬럼은 DECIMAL(9, 6) 이다. 지정하지 않으면 Double 을 FLOAT 로 보고 스키마 검증에 실패한다
    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "latitude")
    private Double latitude;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "location_updated_at")
    private Instant locationUpdatedAt;

    // 매니페스트가 마지막으로 배정한 Edge
    @Column(name = "assigned_edge_id")
    private String assignedEdgeId;

    // 최근 업데이트 결과에서 차량이 실제로 파일을 받은 Edge
    @Column(name = "downloaded_edge_id")
    private String downloadedEdgeId;
}
