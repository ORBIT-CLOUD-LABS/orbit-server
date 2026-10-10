package com.orbit.server.domain.campaign.model;

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
}
