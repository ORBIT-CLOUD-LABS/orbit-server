package com.orbit.server.domain.campaign.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CampaignTargetTest {

    @Test
    @DisplayName("CAMPAIGN-REQ-001: HW 버전과 지역을 생략하면 전체 대상으로 생성한다")
    void createsWithoutHwVersionsAndRegions() {
        assertThatCode(() -> new CampaignTarget("model-a", null, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 대상 차종이 없으면 거부한다")
    void rejectsMissingModel() {
        assertThatThrownBy(() -> new CampaignTarget(" ", Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 빈 HW 버전 값이 있으면 거부한다")
    void rejectsBlankHwVersion() {
        assertThatThrownBy(() -> new CampaignTarget("model-a", Set.of(""), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
