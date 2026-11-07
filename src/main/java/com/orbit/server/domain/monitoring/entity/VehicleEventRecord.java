package com.orbit.server.domain.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;

/**
 * 차량 업데이트 이력이 보여 주는 업데이트 시도 한 번. vehicle_event 테이블을 읽기만 한다.
 *
 * <p>update 컨텍스트의 엔티티를 참조하지 않고 이력에 필요한 컬럼만 매핑한다. 다른 컨텍스트의 테이블이라 쓰기를 막는다. 결과와 실패 사유는
 * update 컨텍스트의 enum 을 참조하지 않도록 문자열로 둔다.
 */
@Entity
@Immutable
@Table(name = "vehicle_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleEventRecord {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "campaign_id")
    private Long campaignId;

    // 이력에 보여 줄 버전은 캠페인이 배포한 artifact 의 버전이다. 캠페인의 artifact 는 바뀌지 않아 이력에 따로 저장하지 않고,
    // 캠페인·artifact 엔티티를 참조하지 않도록 테이블을 직접 읽는다
    @Formula("(SELECT a.version FROM campaign c JOIN artifact a ON a.id = c.artifact_id WHERE c.id = campaign_id)")
    private String version;

    @Column(name = "result")
    private String result;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "failure_detail")
    private String failureDetail;

    @Column(name = "edge_id")
    private String edgeId;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
