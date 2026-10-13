package com.orbit.server.domain.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.campaign.entity.CampaignStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CampaignFilterTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("#18: 값 없이 온 차종 필터는 거르지 않는 것으로 본다")
    void blankModelMeansNoFilter(String model) {
        CampaignFilter filter = new CampaignFilter(CampaignStatus.ACTIVE, model);

        assertThat(filter.model()).isNull();
    }

    @Test
    @DisplayName("#18: 값이 있는 차종 필터는 그대로 둔다")
    void keepsModel() {
        CampaignFilter filter = new CampaignFilter(null, "model-a");

        assertThat(filter.model()).isEqualTo("model-a");
    }
}
