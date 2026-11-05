package com.orbit.server.domain.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * 캠페인 진행 현황이 집계하는 업데이트 결과. update_result 테이블을 읽기만 한다.
 *
 * <p>update 컨텍스트의 엔티티를 참조하지 않고 집계에 필요한 컬럼만 매핑한다. 다른 컨텍스트의 테이블이라 쓰기를 막는다. 결과와 실패 사유는
 * update 컨텍스트의 enum 을 참조하지 않도록 문자열로 둔다.
 */
@Entity
@Immutable
@Table(name = "update_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignResultRecord {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "result")
    private String result;

    @Column(name = "failure_reason")
    private String failureReason;
}
