package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SentVehicleStatusesTest {

    private static final Instant SINCE = Instant.parse("2026-10-01T12:00:00Z");
    private static final Instant FIRST = Instant.parse("2026-10-01T12:00:01Z");
    private static final Instant SECOND = Instant.parse("2026-10-01T12:00:02Z");
    private static final Instant AS_OF = Instant.parse("2026-10-01T12:00:03Z");

    private final SentVehicleStatuses sentStatuses = new SentVehicleStatuses();

    @Test
    @DisplayName("MONITORING-REQ-001: 처음 받은 변경분은 기준 시각과 함께 모두 남긴다")
    void keepsFirstChanges() {
        VehicleStatusChanges changes = changes(status("vehicle-001", FIRST), status("vehicle-002", FIRST));

        assertThat(sentStatuses.keepUnsent(SINCE, changes)).isEqualTo(changes);
    }

    @Test
    @DisplayName("MONITORING-POL-001: 이미 보낸 상태가 겹침 조회로 다시 오면 거른다")
    void filtersAlreadySentStatus() {
        sentStatuses.keepUnsent(SINCE, changes(status("vehicle-001", FIRST)));

        VehicleStatusChanges unsent = sentStatuses.keepUnsent(SINCE, changes(status("vehicle-001", FIRST)));

        assertThat(unsent.asOf()).isEqualTo(AS_OF);
        assertThat(unsent.vehicles()).isEmpty();
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 이미 보낸 차량이라도 상태가 다시 바뀌면 남긴다")
    void keepsChangedStatusOfSentVehicle() {
        sentStatuses.keepUnsent(SINCE, changes(status("vehicle-001", FIRST)));

        VehicleStatusChanges unsent =
                sentStatuses.keepUnsent(SINCE, changes(status("vehicle-001", FIRST), status("vehicle-001", SECOND)));

        assertThat(unsent.vehicles()).containsExactly(status("vehicle-001", SECOND));
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 변경 시각이 같아도 내용이 다르면 남긴다")
    void keepsDifferentStatusWithSameUpdatedAt() {
        sentStatuses.keepUnsent(SINCE, changes(status("vehicle-001", FIRST)));
        VehicleStatus sameTimeNewVersion =
                new VehicleStatus("vehicle-001", "model-a", "1.0.1", FIRST, FIRST, null, null, null);

        VehicleStatusChanges unsent = sentStatuses.keepUnsent(SINCE, changes(sameTimeNewVersion));

        assertThat(unsent.vehicles()).containsExactly(sameTimeNewVersion);
    }

    @Test
    @DisplayName("MONITORING-POL-001: 늦게 커밋돼 이전 변경보다 이른 시각으로 처음 보이는 상태는 남긴다")
    void keepsLateCommittedStatus() {
        sentStatuses.keepUnsent(SINCE, changes(status("vehicle-002", SECOND)));

        VehicleStatusChanges unsent =
                sentStatuses.keepUnsent(SINCE, changes(status("vehicle-001", FIRST), status("vehicle-002", SECOND)));

        assertThat(unsent.vehicles()).containsExactly(status("vehicle-001", FIRST));
    }

    @Test
    @DisplayName("MONITORING-POL-001: 조회 하한 이하로 밀려난 기록은 지운다")
    void forgetsStatusAtOrBeforeSince() {
        sentStatuses.keepUnsent(SINCE, changes(status("vehicle-001", FIRST)));
        sentStatuses.keepUnsent(FIRST, changes());

        VehicleStatusChanges unsent = sentStatuses.keepUnsent(SINCE, changes(status("vehicle-001", FIRST)));

        assertThat(unsent.vehicles()).containsExactly(status("vehicle-001", FIRST));
    }

    private VehicleStatusChanges changes(VehicleStatus... statuses) {
        return new VehicleStatusChanges(AS_OF, List.of(statuses));
    }

    private VehicleStatus status(String vehicleId, Instant updatedAt) {
        return new VehicleStatus(vehicleId, "model-a", "1.0.0", updatedAt, updatedAt, null, null, null);
    }
}
