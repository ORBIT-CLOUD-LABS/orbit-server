package com.orbit.server.domain.campaign.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.Instant;
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
     * @param currentVersionRange 대상 차량의 현재 버전 범위. null 불가 (범위 제한이 없으면 {@link VersionRange#unbounded()})
     * @param period 적용 기간. null 불가
     */
    public CampaignCondition(CampaignTarget target, VersionRange currentVersionRange, CampaignPeriod period) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentVersionRange = Objects.requireNonNull(currentVersionRange, "currentVersionRange");
        this.period = Objects.requireNonNull(period, "period");
    }

    /**
     * 적용 기간을 뺀 대상 조건(차종·HW·지역·현재 버전 범위)이 같은지 판정한다. (CAMPAIGN-POL-001)
     *
     * @param other 비교할 조건
     * @return 대상 조건이 모두 같으면 true
     */
    boolean hasSameTargetAs(CampaignCondition other) {
        return target.equals(other.target) && currentVersionRange().equals(other.currentVersionRange());
    }

    /**
     * 차량이 대상 조건과 적용 기간을 모두 만족하는지 판정한다. (VEHICLE-POL-001)
     *
     * @param vehicle 판정할 차량
     * @param now 판정 시각
     * @return 차종·HW·지역·현재 버전 범위·적용 기간을 모두 만족하면 true
     */
    boolean matches(TargetVehicle vehicle, Instant now) {
        return target.includes(vehicle)
                && currentVersionRange().contains(vehicle.currentVersion())
                && period.contains(now);
    }

    // Hibernate 는 컬럼이 모두 NULL 인 embeddable 을 null 로 채우므로, 필드를 직접 쓰지 않고 이 메서드로 읽는다
    VersionRange currentVersionRange() {
        if (currentVersionRange == null) {
            return VersionRange.unbounded();
        }
        return currentVersionRange;
    }
}
