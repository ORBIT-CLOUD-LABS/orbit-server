package com.orbit.server.domain.campaign.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CampaignTargetTest {

    @Test
    @DisplayName("CAMPAIGN-REQ-001: HW 버전과 지역을 생략하면 전체 대상으로 생성한다")
    void createsWithoutHwVersionsAndRegions() {
        assertThatCode(() -> new CampaignTarget("model-a", null, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 대상 차종이 없으면 거부한다")
    void rejectsMissingModel() {
        assertThatThrownBy(() -> new CampaignTarget(" ", Set.of(), Set.of())).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("CAMPAIGN-REQ-001: 빈 HW 버전 값이 있으면 거부한다")
    void rejectsBlankHwVersion() {
        assertThatThrownBy(() -> new CampaignTarget("model-a", Set.of(""), Set.of()))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("VEHICLE-POL-001: 차종이 같고 HW 버전·지역이 대상에 들면 대상이다")
    void includesMatchingVehicle() {
        CampaignTarget target = new CampaignTarget("model-a", Set.of("hw-1", "hw-2"), Set.of("KR"));

        assertThat(target.includes(vehicle("model-a", "hw-2", "KR"))).isTrue();
    }

    @Test
    @DisplayName("VEHICLE-POL-001: HW 버전·지역을 생략한 캠페인은 차종만 같으면 대상이다")
    void includesAnyHwVersionAndRegionWhenOmitted() {
        CampaignTarget target = new CampaignTarget("model-a", null, null);

        assertThat(target.includes(vehicle("model-a", "hw-9", "US"))).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"model-b, hw-1, KR", "model-a, hw-3, KR", "model-a, hw-1, US"})
    @DisplayName("VEHICLE-POL-001: 차종·HW 버전·지역 중 하나라도 다르면 대상이 아니다")
    void excludesVehicleOutOfTarget(String model, String hwVersion, String region) {
        CampaignTarget target = new CampaignTarget("model-a", Set.of("hw-1", "hw-2"), Set.of("KR"));

        assertThat(target.includes(vehicle(model, hwVersion, region))).isFalse();
    }

    private TargetVehicle vehicle(String model, String hwVersion, String region) {
        return new TargetVehicle(model, hwVersion, region, "1.0.0");
    }
}
