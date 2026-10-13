package com.orbit.server.domain.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.update.service.CheckInCommand;
import com.orbit.server.domain.update.service.CheckInService;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.service.RegisterVehicleCommand;
import com.orbit.server.domain.vehicle.service.VehicleRegistrationService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * updated_at 은 커밋이 아니라 UPDATE 실행 시각이라, 변경분 조회 때 커밋 전이던 체크인이 기준 시각보다 이른 updated_at 으로 늦게 커밋될 수
 * 있다. 겹침 조회와 체크인 트랜잭션 타임아웃이 함께 이 변경을 놓치지 않게 하는지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CheckInLateCommitTest {

    private static final String VEHICLE_ID = "vehicle-001";

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private VehicleRegistrationService registrationService;

    @Autowired
    private VehicleStatusQuery vehicleStatusQuery;

    @Autowired
    private DashboardStreamProperties streamProperties;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        registrationService.register(new RegisterVehicleCommand(
                new VehicleExternalId(VEHICLE_ID), new VehicleSpec("model-a", "hw-1", "KR"), "1.0.0"));
    }

    // 커밋 순서를 직접 다루느라 트랜잭션 롤백에 기대지 않으므로 지운다. 상태는 FK 의 ON DELETE CASCADE 로 함께 지워진다
    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM vehicle");
    }

    @Test
    @DisplayName("MONITORING-POL-001: 변경분 조회 때 커밋 전이던 체크인도 커밋 뒤 다음 겹침 조회에서 받는다")
    void receivesCheckInCommittedAfterChangesQuery() {
        Instant previousAsOf =
                vehicleStatusQuery.findChangesSince(Instant.EPOCH).asOf();

        VehicleStatusChanges changesBeforeCommit = checkInPausedBeforeCommit(previousAsOf);

        Instant cursor = changesBeforeCommit.asOf();
        assertThat(changesBeforeCommit.vehicles()).isEmpty();
        assertThat(vehicleStatusQuery.findChangesSince(cursor).vehicles()).isEmpty();
        assertThat(vehicleStatusQuery
                        .findChangesSince(cursor.minus(streamProperties.overlap()))
                        .vehicles())
                .singleElement()
                .extracting(VehicleStatus::currentVersion)
                .isEqualTo("1.0.1");
    }

    @Test
    @DisplayName("MONITORING-POL-001: 체크인 트랜잭션 타임아웃은 설정할 수 있는 가장 짧은 겹침 시간보다 짧다")
    void checkInTimeoutIsShorterThanOverlap() throws NoSuchMethodException {
        TransactionDefinition checkIn = new AnnotationTransactionAttributeSource()
                .getTransactionAttribute(
                        CheckInService.class.getMethod("checkIn", CheckInCommand.class), CheckInService.class);

        assertThat(checkIn.getTimeout()).isPositive();
        assertThat(Duration.ofSeconds(checkIn.getTimeout()))
                .isLessThan(Duration.ofSeconds(DashboardStreamProperties.MIN_OVERLAP_SECONDS));
    }

    // 체크인을 바깥 트랜잭션에 묶어 커밋을 미루고, 그동안 다른 연결로 변경분을 조회한다
    private VehicleStatusChanges checkInPausedBeforeCommit(Instant since) {
        TransactionTemplate checkInTransaction = new TransactionTemplate(transactionManager);
        TransactionTemplate otherConnection = new TransactionTemplate(transactionManager);
        otherConnection.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return checkInTransaction.execute(status -> {
            checkInService.checkIn(new CheckInCommand(VEHICLE_ID, "1.0.1"));
            return otherConnection.execute(other -> vehicleStatusQuery.findChangesSince(since));
        });
    }
}
