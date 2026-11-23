package com.orbit.server.domain.vehicle.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.vehicle.domain.VehicleExternalId;
import com.orbit.server.domain.vehicle.domain.VehicleSpec;
import com.orbit.server.domain.vehicle.service.command.RegisterVehicleCommand;
import com.orbit.server.domain.vehicle.service.result.VehicleRegistrationResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 동시 재등록은 요청마다 별도 트랜잭션이 커밋돼야 재현되므로, 테스트를 트랜잭션으로 묶지 않고 실제 등록 흐름으로 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class VehicleRegistrationConcurrencyTest {

    private static final int REQUEST_COUNT = 5;
    // 이웃한 식별자의 최초 등록이 같은 인덱스 gap 을 두고 겹치도록 충분히 많이 동시에 보낸다
    private static final int NEW_VEHICLE_COUNT = 20;
    private static final VehicleSpec SPEC = new VehicleSpec("model-a", "hw-1", "KR");
    private static final RegisterVehicleCommand COMMAND =
            new RegisterVehicleCommand(new VehicleExternalId("vehicle-001"), SPEC, "1.0.0");

    @Autowired
    private VehicleRegistrationService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        service.register(COMMAND);
    }

    // 트랜잭션 롤백에 기대지 않으므로 다른 DB 테스트에 데이터가 남지 않게 지운다. 토큰·상태는 FK 의 ON DELETE CASCADE 로 함께 지워진다
    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM vehicle");
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 같은 차량의 재등록이 동시에 와도 모두 성공하고 유효 토큰은 하나만 남는다")
    void keepsSingleActiveTokenOnConcurrentReregistration() throws Exception {
        List<VehicleRegistrationResult> results =
                resultsOf(registerConcurrently(Collections.nCopies(REQUEST_COUNT, COMMAND)));

        assertThat(results).hasSize(REQUEST_COUNT).noneMatch(VehicleRegistrationResult::created);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM vehicle_credential WHERE status = 'ACTIVE'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vehicle_credential", Integer.class))
                .isEqualTo(REQUEST_COUNT + 1);
    }

    @Test
    @DisplayName("VEHICLE-REQ-002: 서로 다른 차량의 최초 등록이 동시에 와도 모두 성공한다")
    void registersAllOnConcurrentFirstRegistrationOfDifferentVehicles() throws Exception {
        List<RegisterVehicleCommand> commands = IntStream.range(0, NEW_VEHICLE_COUNT)
                .mapToObj(i -> new RegisterVehicleCommand(new VehicleExternalId("new-vehicle-" + i), SPEC, "1.0.0"))
                .toList();

        List<VehicleRegistrationResult> results = resultsOf(registerConcurrently(commands));

        assertThat(results).hasSize(NEW_VEHICLE_COUNT).allMatch(VehicleRegistrationResult::created);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM vehicle_credential WHERE status = 'ACTIVE'", Integer.class))
                .isEqualTo(NEW_VEHICLE_COUNT + 1);
    }

    // 요청이 실제로 겹쳤는지는 스케줄링에 달려 있어, 겹치지 않아 모두 성공한 경우도 허용한다.
    // 겹쳤다면 진 쪽은 데드락이 아니라 UNIQUE 제약 위반으로 실패해야 차량이 재시도할 수 있는 409 로 응답한다
    @Test
    @DisplayName("VEHICLE-REQ-002: 같은 차량의 최초 등록이 동시에 오면 하나만 생성하고 나머지는 성공하거나 UNIQUE 제약 위반으로 실패한다")
    void createsOnceOnConcurrentFirstRegistrationOfSameVehicle() throws Exception {
        RegisterVehicleCommand command =
                new RegisterVehicleCommand(new VehicleExternalId("new-vehicle"), SPEC, "1.0.0");

        List<Future<VehicleRegistrationResult>> futures =
                registerConcurrently(Collections.nCopies(REQUEST_COUNT, command));

        assertThat(futures)
                .filteredOn(future -> future.state() == Future.State.FAILED)
                .allSatisfy(future ->
                        assertThat(future.exceptionNow()).isInstanceOf(DataIntegrityViolationException.class));
        assertThat(futures)
                .filteredOn(future -> future.state() == Future.State.SUCCESS)
                .map(Future::resultNow)
                .filteredOn(VehicleRegistrationResult::created)
                .hasSize(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM vehicle WHERE external_id = 'new-vehicle'", Integer.class))
                .isEqualTo(1);
    }

    // 모든 요청이 출발선에서 기다리게 한 뒤 한꺼번에 풀어 잠금 구간이 겹치게 한다. 모든 요청이 끝난 뒤 돌려준다
    private List<Future<VehicleRegistrationResult>> registerConcurrently(List<RegisterVehicleCommand> commands) {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(commands.size())) {
            List<Future<VehicleRegistrationResult>> futures = commands.stream()
                    .map(command -> executor.submit(waitingRequest(start, command)))
                    .toList();
            start.countDown();
            return futures;
        }
    }

    private Callable<VehicleRegistrationResult> waitingRequest(CountDownLatch start, RegisterVehicleCommand command) {
        return () -> {
            start.await();
            return service.register(command);
        };
    }

    // 실패한 요청이 있으면 그 원인 예외로 테스트를 실패시킨다
    private List<VehicleRegistrationResult> resultsOf(List<Future<VehicleRegistrationResult>> futures)
            throws Exception {
        List<VehicleRegistrationResult> results = new ArrayList<>();
        for (Future<VehicleRegistrationResult> future : futures) {
            results.add(future.get());
        }
        return results;
    }
}
