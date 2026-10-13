package com.orbit.server.domain.vehicle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.vehicle.entity.Vehicle;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.repository.VehicleRepository;
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
class VehicleQueryServiceTest {

    @Autowired
    private VehicleQueryService service;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    @DisplayName("VEHICLE-REQ-001: 차량 식별자로 차량 ID 와 차종·HW 버전·지역을 조회한다")
    void getsProfileByExternalId() {
        Vehicle vehicle = vehicleRepository.save(
                new Vehicle(new VehicleExternalId("vehicle-001"), new VehicleSpec("model-a", "hw-1", "KR")));

        assertThat(service.getProfile("vehicle-001"))
                .isEqualTo(new VehicleProfile(vehicle.id(), "model-a", "hw-1", "KR"));
    }

    @Test
    @DisplayName("VEHICLE-REQ-001: 등록하지 않은 차량이면 데이터가 어긋난 것이므로 서버 오류다")
    void failsForUnregisteredVehicle() {
        assertThatThrownBy(() -> service.getProfile("vehicle-404")).isInstanceOf(IllegalStateException.class);
    }
}
