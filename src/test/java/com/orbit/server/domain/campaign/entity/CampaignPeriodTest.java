package com.orbit.server.domain.campaign.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CampaignPeriodTest {

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Duration ONE_DAY = Duration.ofDays(1);

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 시작 시각이 종료 시각보다 이르면 생성한다")
    void createsWhenStartIsBeforeEnd() {
        assertThatCode(() -> new CampaignPeriod(START, START.plus(ONE_DAY))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 시작 시각과 종료 시각이 같으면 거부한다")
    void rejectsSameStartAndEnd() {
        assertThatThrownBy(() -> new CampaignPeriod(START, START)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 종료 시각이 시작 시각보다 이르면 거부한다")
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> new CampaignPeriod(START, START.minus(ONE_DAY)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 시각이 없으면 거부한다")
    void rejectsMissingTime() {
        assertThatThrownBy(() -> new CampaignPeriod(null, START)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 시작 시각부터 종료 직전까지가 적용 기간이다")
    void containsFromStartUntilBeforeEnd() {
        CampaignPeriod period = new CampaignPeriod(START, START.plus(ONE_DAY));

        assertThat(period.contains(START)).isTrue();
        assertThat(period.contains(START.plus(ONE_DAY).minusMillis(1))).isTrue();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 시작 전이나 종료 시각부터는 적용 기간이 아니다")
    void excludesBeforeStartAndFromEnd() {
        CampaignPeriod period = new CampaignPeriod(START, START.plus(ONE_DAY));

        assertThat(period.contains(START.minusMillis(1))).isFalse();
        assertThat(period.contains(START.plus(ONE_DAY))).isFalse();
    }
}
