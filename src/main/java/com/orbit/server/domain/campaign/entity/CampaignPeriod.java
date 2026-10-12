package com.orbit.server.domain.campaign.entity;

import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * 캠페인 적용 기간. 기간 밖의 차량은 조건이 맞아도 대상이 아니다.
 *
 * <p>DB 기본값으로 채우는 시각과 비교하므로 JVM 시간대와 무관한 {@link Instant} 로 둔다.
 *
 * @param startAt 시작 시각. null 불가
 * @param endAt 종료 시각. null 불가, 시작 시각 이후
 */
@Embeddable
public record CampaignPeriod(
        @Column(name = "start_at") Instant startAt,
        @Column(name = "end_at") Instant endAt) {

    /**
     * @throws InvalidValueException 시각이 없거나 종료 시각이 시작 시각보다 늦지 않은 경우
     */
    public CampaignPeriod {
        if (startAt == null || endAt == null) {
            throw new InvalidValueException("캠페인 시작·종료 시각은 비어 있을 수 없습니다.");
        }
        if (!startAt.isBefore(endAt)) {
            throw new InvalidValueException("캠페인 종료 시각은 시작 시각보다 늦어야 합니다.");
        }
    }

    /**
     * 시각이 적용 기간 안인지 판정한다. 기간이 이어지는 두 캠페인이 같은 시각에 함께 적용되지 않도록 종료 시각은 포함하지 않는다.
     * (VEHICLE-POL-001)
     *
     * @param time 판정할 시각
     * @return 시작 시각 이상, 종료 시각 미만이면 true
     */
    boolean contains(Instant time) {
        return !time.isBefore(startAt) && time.isBefore(endAt);
    }
}
