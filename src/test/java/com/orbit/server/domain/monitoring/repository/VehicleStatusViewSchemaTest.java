package com.orbit.server.domain.monitoring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 차량 상세가 읽는 컬럼과 [Phase2] 위치·Edge 컬럼이 차량 상태 뷰에 나오는지 SQL 로 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class VehicleStatusViewSchemaTest {

    @Autowired
    private EntityManager em;

    private Long vehicleId;

    @BeforeEach
    void setUp() {
        em.createNativeQuery("INSERT INTO vehicle (external_id, model, hw_version, region)"
                        + " VALUES ('vehicle-001', 'model-a', 'hw-1', 'KR')")
                .executeUpdate();
        vehicleId = lastInsertId();
        em.createNativeQuery("INSERT INTO vehicle_state (vehicle_id, current_version) VALUES (:id, '1.0.0')")
                .setParameter("id", vehicleId)
                .executeUpdate();
    }

    @Test
    @DisplayName("위치를 받은 적 없고 업데이트 결과도 없으면 위치·Edge 컬럼은 비어 있다")
    void showsEmptyLocationAndEdges() {
        assertThat(locationAndEdges()).isEqualTo("null:null:null:null:null");
    }

    @Test
    @DisplayName("차량 상태의 위치·배정 Edge 를 보여 준다")
    void showsLocationAndAssignedEdge() {
        em.createNativeQuery("UPDATE vehicle_state SET latitude = 37.566500, longitude = 126.978000,"
                        + " location_updated_at = '2026-10-01 12:00:00', assigned_edge_id = 'edge-seoul'"
                        + " WHERE vehicle_id = :id")
                .setParameter("id", vehicleId)
                .executeUpdate();

        assertThat(locationAndEdges()).isEqualTo("37.566500:126.978000:2026-10-01 12:00:00:edge-seoul:null");
    }

    @Test
    @DisplayName("다운로드 Edge 는 가장 최근 업데이트 결과의 Edge 를 보여 준다")
    void showsEdgeOfLatestResult() {
        insertResult("1.0.1", "edge-seoul", "2026-10-01 12:00:00");
        insertResult("1.0.2", "edge-daejeon", "2026-10-01 12:05:00");

        assertThat(locationAndEdges()).endsWith(":edge-daejeon");
    }

    @Test
    @DisplayName("차량의 HW 버전·지역과 가장 최근 업데이트 결과의 실패 상세·종료 시각을 보여 준다")
    void showsVehicleSpecAndLatestResultDetail() {
        insertResult("1.0.1", "edge-seoul", "2026-10-01 12:00:00");
        insertFailedResult("1.0.2", "unknown keyId: v2", "2026-10-01 12:05:00");

        assertThat(specAndResultDetail()).isEqualTo("hw-1:KR:unknown keyId: v2:2026-10-01 12:05:00");
    }

    @Test
    @DisplayName("업데이트 결과가 없으면 실패 상세·종료 시각은 비어 있다")
    void showsEmptyResultDetailWithoutResult() {
        assertThat(specAndResultDetail()).isEqualTo("hw-1:KR:null:null");
    }

    @Test
    @DisplayName("SSE 변경분 조회가 인덱스를 타도록 뷰를 MERGE 로 처리한다")
    void keepsMergeAlgorithm() {
        Object[] definition = (Object[])
                em.createNativeQuery("SHOW CREATE VIEW vehicle_status").getSingleResult();

        assertThat((String) definition[1]).contains("ALGORITHM=MERGE");
    }

    private void insertResult(String version, String edgeId, String reportedAt) {
        insertCampaign(version);
        em.createNativeQuery("INSERT INTO update_result"
                        + " (vehicle_id, campaign_id, result, finished_at, reported_at, edge_id)"
                        + " VALUES (:vehicleId, LAST_INSERT_ID(), 'SUCCEEDED', :reportedAt, :reportedAt, :edgeId)")
                .setParameter("vehicleId", vehicleId)
                .setParameter("reportedAt", reportedAt)
                .setParameter("edgeId", edgeId)
                .executeUpdate();
    }

    private void insertFailedResult(String version, String failureDetail, String reportedAt) {
        insertCampaign(version);
        em.createNativeQuery("INSERT INTO update_result"
                        + " (vehicle_id, campaign_id, result, failure_reason, failure_detail, finished_at, reported_at)"
                        + " VALUES (:vehicleId, LAST_INSERT_ID(), 'FAILED', 'SIGNATURE_INVALID', :failureDetail,"
                        + " :reportedAt, :reportedAt)")
                .setParameter("vehicleId", vehicleId)
                .setParameter("failureDetail", failureDetail)
                .setParameter("reportedAt", reportedAt)
                .executeUpdate();
    }

    private void insertCampaign(String version) {
        em.createNativeQuery("INSERT INTO artifact (model, version, sha256, size_bytes, path)"
                        + " VALUES ('model-a', :version, :sha256, 1, :path)")
                .setParameter("version", version)
                .setParameter("sha256", "a".repeat(64))
                .setParameter("path", "/firmware/model-a/" + version)
                .executeUpdate();
        em.createNativeQuery("INSERT INTO campaign (artifact_id, name, model, start_at, end_at, status)"
                        + " VALUES (LAST_INSERT_ID(), 'campaign', 'model-a',"
                        + " '2026-10-01 00:00:00', '2026-10-02 00:00:00', 'ACTIVE')")
                .executeUpdate();
    }

    private String specAndResultDetail() {
        // 시각은 JVM 시간대로 바뀌지 않도록 DB 에서 문자열로 읽는다
        Object[] row = (Object[]) em.createNativeQuery("SELECT hw_version, region, failure_detail,"
                        + " DATE_FORMAT(finished_at, '%Y-%m-%d %H:%i:%s')"
                        + " FROM vehicle_status WHERE external_id = 'vehicle-001'")
                .getSingleResult();
        return row[0] + ":" + row[1] + ":" + row[2] + ":" + row[3];
    }

    private String locationAndEdges() {
        // 시각은 JVM 시간대로 바뀌지 않도록 DB 에서 문자열로 읽는다
        Object[] row = (Object[]) em.createNativeQuery("SELECT latitude, longitude,"
                        + " DATE_FORMAT(location_updated_at, '%Y-%m-%d %H:%i:%s'), assigned_edge_id, downloaded_edge_id"
                        + " FROM vehicle_status WHERE external_id = 'vehicle-001'")
                .getSingleResult();
        return row[0] + ":" + row[1] + ":" + row[2] + ":" + row[3] + ":" + row[4];
    }

    private Long lastInsertId() {
        return ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }
}
