package com.orbit.server.domain.vehicle.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
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
        List<VehicleRegistrationResult> results = registerConcurrently(Collections.nCopies(REQUEST_COUNT, COMMAND));

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

        List<VehicleRegistrationResult> results = registerConcurrently(commands);

        assertThat(results).hasSize(NEW_VEHICLE_COUNT).allMatch(VehicleRegistrationResult::created);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM vehicle_credential WHERE status = 'ACTIVE'", Integer.class))
                .isEqualTo(NEW_VEHICLE_COUNT + 1);
    }

    // 모든 요청이 출발선에서 기다리게 한 뒤 한꺼번에 풀어 잠금 구간이 겹치게 한다
    private List<VehicleRegistrationResult> registerConcurrently(List<RegisterVehicleCommand> commands)
            throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(commands.size())) {
            List<Future<VehicleRegistrationResult>> futures = commands.stream()
                    .map(command -> executor.submit(waitingRequest(start, command)))
                    .toList();
            start.countDown();
            List<VehicleRegistrationResult> results = new ArrayList<>();
            for (Future<VehicleRegistrationResult> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }

    private Callable<VehicleRegistrationResult> waitingRequest(CountDownLatch start, RegisterVehicleCommand command) {
        return () -> {
            start.await();
            return service.register(command);
        };
    }
}
