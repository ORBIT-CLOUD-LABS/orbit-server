package com.orbit.server.domain.campaign.model;

import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

/**
 * 캠페인 적용 기간. 기간 밖의 차량은 조건이 맞아도 대상이 아니다.
 *
 * @param startAt 시작 시각. null 불가
 * @param endAt 종료 시각. null 불가, 시작 시각 이후
 */
@Embeddable
public record CampaignPeriod(
        @Column(name = "start_at") LocalDateTime startAt,
        @Column(name = "end_at") LocalDateTime endAt) {

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
}
