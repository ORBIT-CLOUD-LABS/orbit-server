package com.orbit.server.domain.campaign.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Objects;

/**
 * 차량이 캠페인 대상인지 판정하는 조건. 차종·HW·지역·현재 버전 범위·적용 기간을 모두 만족해야 대상이다.
 *
 * <p>판정 규칙이 이 조건들을 함께 보므로 하나로 묶는다.
 */
@Embeddable
public class CampaignCondition {

    @Embedded
    private CampaignTarget target;

    @Embedded
    private VersionRange currentVersionRange;

    @Embedded
    private CampaignPeriod period;

    protected CampaignCondition() {}

    /**
     * @param target 대상 차종·HW 버전·지역. null 불가
     * @param currentVersionRange 대상 차량의 현재 버전 범위. null 불가 (범위 제한이 없으면 양 끝이 null 인 범위)
     * @param period 적용 기간. null 불가
     */
    public CampaignCondition(CampaignTarget target, VersionRange currentVersionRange, CampaignPeriod period) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentVersionRange = Objects.requireNonNull(currentVersionRange, "currentVersionRange");
        this.period = Objects.requireNonNull(period, "period");
    }
}
