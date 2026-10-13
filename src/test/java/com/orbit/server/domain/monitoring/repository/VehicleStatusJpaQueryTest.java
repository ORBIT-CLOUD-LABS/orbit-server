package com.orbit.server.domain.monitoring.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.monitoring.service.VehicleStatus;
import com.orbit.server.domain.monitoring.service.VehicleStatusChanges;
import com.orbit.server.domain.monitoring.service.VehicleStatusQuery;
import com.orbit.server.domain.monitoring.service.VehicleStatusSnapshot;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class VehicleStatusJpaQueryTest {

    private static final Instant EARLIER = Instant.parse("2026-10-01T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-01T12:05:00Z");

    @Autowired
    private VehicleStatusQuery vehicleStatusQuery;

    // monitoring 은 뷰로 테이블만 읽으므로 다른 컨텍스트의 엔티티 대신 행을 바로 넣어 보고 시각까지 정한다
    @Autowired
    private EntityManager em;

    private Long firstCampaignId;
    private Long secondCampaignId;

    @BeforeEach
    void setUp() {
        firstCampaignId = insertCampaign("1.0.1");
        secondCampaignId = insertCampaign("1.0.2");
    }

    @Test
    @DisplayName("MONITORING-REQ-004: 차량이 없으면 기준 시각과 빈 목록을 응답한다")
    void returnsEmptyVehiclesWithAsOf() {
        VehicleStatusSnapshot snapshot = vehicleStatusQuery.findSnapshot();

        assertThat(snapshot.asOf()).isNotNull();
        assertThat(snapshot.vehicles()).isEmpty();
    }

    @Test
    @DisplayName("MONITORING-REQ-004: 업데이트 결과가 없는 차량은 결과 필드를 비워서 응답한다")
    void returnsVehicleWithoutUpdateResult() {
        Long vehicleId = insertVehicle("vehicle-001");
        checkIn(vehicleId);

        VehicleStatusSnapshot snapshot = vehicleStatusQuery.findSnapshot();

        assertThat(snapshot.vehicles()).singleElement().satisfies(status -> {
            assertThat(status.vehicleId()).isEqualTo("vehicle-001");
            assertThat(status.model()).isEqualTo("model-a");
            assertThat(status.currentVersion()).isEqualTo("1.0.0");
            assertThat(status.lastSeenAt()).isEqualTo(EARLIER);
            assertThat(status.updatedAt()).isNotNull();
            assertThat(status.campaignId()).isNull();
            assertThat(status.lastResult()).isNull();
            assertThat(status.failureReason()).isNull();
        });
    }

    @Test
    @DisplayName("MONITORING-REQ-004: 체크인한 적 없는 차량은 마지막 체크인 시각을 비워서 응답한다")
    void returnsVehicleNeverCheckedIn() {
        insertVehicle("vehicle-001");

        VehicleStatusSnapshot snapshot = vehicleStatusQuery.findSnapshot();

        assertThat(snapshot.vehicles())
                .singleElement()
                .extracting(VehicleStatus::lastSeenAt)
                .isNull();
    }

    @Test
    @DisplayName("MONITORING-POL-003: 실패한 업데이트는 캠페인과 실패 사유를 함께 응답한다")
    void returnsFailedUpdateResult() {
        Long vehicleId = insertVehicle("vehicle-001");
        insertFailed(vehicleId, firstCampaignId, EARLIER);

        VehicleStatusSnapshot snapshot = vehicleStatusQuery.findSnapshot();

        assertThat(snapshot.vehicles()).singleElement().satisfies(status -> {
            assertThat(status.campaignId()).isEqualTo(firstCampaignId);
            assertThat(status.lastResult()).isEqualTo("FAILED");
            assertThat(status.failureReason()).isEqualTo("HASH_MISMATCH");
        });
    }

    @Test
    @DisplayName("MONITORING-POL-003: 업데이트 결과가 여러 개면 가장 늦게 보고된 결과를 응답한다")
    void returnsLatestReportedResult() {
        Long vehicleId = insertVehicle("vehicle-001");
        insertSucceeded(vehicleId, secondCampaignId, LATER);
        insertFailed(vehicleId, firstCampaignId, EARLIER);

        VehicleStatusSnapshot snapshot = vehicleStatusQuery.findSnapshot();

        assertThat(snapshot.vehicles()).singleElement().satisfies(status -> {
            assertThat(status.campaignId()).isEqualTo(secondCampaignId);
            assertThat(status.lastResult()).isEqualTo("SUCCEEDED");
            assertThat(status.failureReason()).isNull();
        });
    }

    @Test
    @DisplayName("MONITORING-POL-003: 보고 시각이 같으면 나중에 기록한 결과를 응답한다")
    void returnsLastRecordedResultOnSameReportedAt() {
        Long vehicleId = insertVehicle("vehicle-001");
        insertFailed(vehicleId, firstCampaignId, EARLIER);
        insertSucceeded(vehicleId, secondCampaignId, EARLIER);

        VehicleStatusSnapshot snapshot = vehicleStatusQuery.findSnapshot();

        assertThat(snapshot.vehicles())
                .singleElement()
                .extracting(VehicleStatus::campaignId)
                .isEqualTo(secondCampaignId);
    }

    @Test
    @DisplayName("MONITORING-REQ-004: 전체 차량을 vehicleId 오름차순으로 응답한다")
    void returnsAllVehiclesOrderedByVehicleId() {
        insertVehicle("vehicle-002");
        insertVehicle("vehicle-001");

        VehicleStatusSnapshot snapshot = vehicleStatusQuery.findSnapshot();

        assertThat(snapshot.vehicles())
                .extracting(VehicleStatus::vehicleId)
                .containsExactly("vehicle-001", "vehicle-002");
    }

    @Test
    @DisplayName("MONITORING-POL-001: 기준 시각은 차량 상태를 읽기 전 DB 시각이라 상태 변경 시각보다 앞서지 않는다")
    void asOfIsNotBeforeUpdatedAt() {
        insertVehicle("vehicle-001");

        VehicleStatusSnapshot snapshot = vehicleStatusQuery.findSnapshot();

        assertThat(snapshot.vehicles())
                .singleElement()
                .satisfies(status -> assertThat(snapshot.asOf()).isAfterOrEqualTo(status.updatedAt()));
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 주어진 시각보다 늦게 바뀐 차량만 변경분으로 응답한다")
    void returnsVehiclesUpdatedAfterSince() {
        changeStateAt(insertVehicle("vehicle-001"), EARLIER);
        changeStateAt(insertVehicle("vehicle-002"), LATER);

        VehicleStatusChanges changes = vehicleStatusQuery.findChangesSince(EARLIER);

        assertThat(changes.vehicles()).extracting(VehicleStatus::vehicleId).containsExactly("vehicle-002");
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 주어진 시각과 같은 시각에 바뀐 차량은 변경분에 넣지 않는다")
    void excludesVehicleUpdatedAtSince() {
        changeStateAt(insertVehicle("vehicle-001"), LATER);

        VehicleStatusChanges changes = vehicleStatusQuery.findChangesSince(LATER);

        assertThat(changes.vehicles()).isEmpty();
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 바뀐 차량이 없으면 기준 시각과 빈 목록을 응답한다")
    void returnsEmptyChangesWithAsOf() {
        changeStateAt(insertVehicle("vehicle-001"), EARLIER);

        VehicleStatusChanges changes = vehicleStatusQuery.findChangesSince(LATER);

        assertThat(changes.asOf()).isNotNull();
        assertThat(changes.vehicles()).isEmpty();
    }

    @Test
    @DisplayName("MONITORING-REQ-001: 변경분은 스냅샷과 같은 형식으로 vehicleId 오름차순으로 응답한다")
    void returnsChangesInSnapshotFormatOrderedByVehicleId() {
        Long secondVehicleId = insertVehicle("vehicle-002");
        insertFailed(secondVehicleId, firstCampaignId, EARLIER);
        changeStateAt(secondVehicleId, LATER);
        changeStateAt(insertVehicle("vehicle-001"), LATER);

        VehicleStatusChanges changes = vehicleStatusQuery.findChangesSince(EARLIER);

        assertThat(changes.vehicles())
                .extracting(VehicleStatus::vehicleId, VehicleStatus::updatedAt, VehicleStatus::lastResult)
                .containsExactly(tuple("vehicle-001", LATER, null), tuple("vehicle-002", LATER, "FAILED"));
    }

    @Test
    @DisplayName("MONITORING-POL-001: 변경분의 기준 시각은 차량 상태를 읽기 전 DB 시각이라 상태 변경 시각보다 앞서지 않는다")
    void changesAsOfIsNotBeforeUpdatedAt() {
        insertVehicle("vehicle-001");

        VehicleStatusChanges changes = vehicleStatusQuery.findChangesSince(EARLIER);

        assertThat(changes.vehicles())
                .singleElement()
                .satisfies(status -> assertThat(changes.asOf()).isAfterOrEqualTo(status.updatedAt()));
    }

    private Long insertCampaign(String version) {
        em.createNativeQuery("INSERT INTO artifact (model, version, sha256, size_bytes, path)"
                        + " VALUES ('model-a', :version, :sha256, 1, :path)")
                .setParameter("version", version)
                .setParameter("sha256", "a".repeat(64))
                .setParameter("path", "/firmware/model-a/" + version)
                .executeUpdate();
        em.createNativeQuery("INSERT INTO campaign (artifact_id, name, model, start_at, end_at, status)"
                        + " VALUES (LAST_INSERT_ID(), 'campaign', 'model-a', :startAt, :endAt, 'ACTIVE')")
                .setParameter("startAt", EARLIER)
                .setParameter("endAt", LATER)
                .executeUpdate();
        return lastInsertId();
    }

    // 차량 등록과 같이 차량과 상태를 함께 만든다
    private Long insertVehicle(String externalId) {
        em.createNativeQuery("INSERT INTO vehicle (external_id, model, hw_version, region)"
                        + " VALUES (:externalId, 'model-a', 'hw-1', 'KR')")
                .setParameter("externalId", externalId)
                .executeUpdate();
        Long vehicleId = lastInsertId();
        em.createNativeQuery("INSERT INTO vehicle_state (vehicle_id, current_version) VALUES (:vehicleId, '1.0.0')")
                .setParameter("vehicleId", vehicleId)
                .executeUpdate();
        return vehicleId;
    }

    private void checkIn(Long vehicleId) {
        em.createNativeQuery("UPDATE vehicle_state SET last_seen_at = :lastSeenAt WHERE vehicle_id = :vehicleId")
                .setParameter("lastSeenAt", EARLIER)
                .setParameter("vehicleId", vehicleId)
                .executeUpdate();
    }

    // updated_at 을 직접 넣으면 ON UPDATE 로 덮이지 않으므로 변경 시각을 정할 수 있다
    private void changeStateAt(Long vehicleId, Instant updatedAt) {
        em.createNativeQuery("UPDATE vehicle_state SET updated_at = :updatedAt WHERE vehicle_id = :vehicleId")
                .setParameter("updatedAt", updatedAt)
                .setParameter("vehicleId", vehicleId)
                .executeUpdate();
    }

    private void insertSucceeded(Long vehicleId, Long campaignId, Instant reportedAt) {
        em.createNativeQuery("INSERT INTO update_result (vehicle_id, campaign_id, result, finished_at, reported_at)"
                        + " VALUES (:vehicleId, :campaignId, 'SUCCEEDED', :reportedAt, :reportedAt)")
                .setParameter("vehicleId", vehicleId)
                .setParameter("campaignId", campaignId)
                .setParameter("reportedAt", reportedAt)
                .executeUpdate();
    }

    private void insertFailed(Long vehicleId, Long campaignId, Instant reportedAt) {
        em.createNativeQuery("INSERT INTO update_result"
                        + " (vehicle_id, campaign_id, result, failure_reason, finished_at, reported_at)"
                        + " VALUES (:vehicleId, :campaignId, 'FAILED', 'HASH_MISMATCH', :reportedAt, :reportedAt)")
                .setParameter("vehicleId", vehicleId)
                .setParameter("campaignId", campaignId)
                .setParameter("reportedAt", reportedAt)
                .executeUpdate();
    }

    private Long lastInsertId() {
        return ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }
}
