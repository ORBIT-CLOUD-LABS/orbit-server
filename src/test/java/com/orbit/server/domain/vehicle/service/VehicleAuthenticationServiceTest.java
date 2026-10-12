package com.orbit.server.domain.vehicle.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleSpec;
import com.orbit.server.domain.vehicle.entity.VehicleToken;
import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.ErrorCode;
import com.orbit.server.global.security.AuthErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
class VehicleAuthenticationServiceTest {

    private static final String VEHICLE_ID = "vehicle-001";

    @Autowired
    private VehicleAuthenticationService service;

    @Autowired
    private VehicleRegistrationService registrationService;

    @Test
    @DisplayName("VEHICLE-POL-003: 경로 차량에 발급한 유효 토큰이면 통과한다")
    void passesActiveTokenOfPathVehicle() {
        String token = register(VEHICLE_ID);

        assertThatCode(() -> service.authenticate(token, VEHICLE_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 재등록으로 폐기한 토큰이면 UNAUTHORIZED")
    void rejectsRevokedToken() {
        String revoked = register(VEHICLE_ID);
        register(VEHICLE_ID);

        assertErrorCode(() -> service.authenticate(revoked, VEHICLE_ID), AuthErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 발급하지 않은 토큰이면 UNAUTHORIZED")
    void rejectsUnknownToken() {
        register(VEHICLE_ID);

        assertErrorCode(
                () -> service.authenticate(VehicleToken.issue().value(), VEHICLE_ID), AuthErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 토큰 형식이 아니면 입력 오류가 아니라 UNAUTHORIZED")
    void rejectsMalformedToken() {
        assertErrorCode(() -> service.authenticate(" ", VEHICLE_ID), AuthErrorCode.UNAUTHORIZED);
        assertErrorCode(() -> service.authenticate("a".repeat(129), VEHICLE_ID), AuthErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("VEHICLE-POL-003: 다른 차량의 토큰이면 VEHICLE_MISMATCH")
    void rejectsTokenOfOtherVehicle() {
        register(VEHICLE_ID);
        String otherToken = register("vehicle-002");

        assertErrorCode(() -> service.authenticate(otherToken, VEHICLE_ID), VehicleErrorCode.VEHICLE_MISMATCH);
    }

    private String register(String vehicleId) {
        RegisterVehicleCommand command = new RegisterVehicleCommand(
                new VehicleExternalId(vehicleId), new VehicleSpec("model-a", "hw-1", "KR"), "1.0.0");
        return registrationService.register(command).vehicleToken().value();
    }

    private void assertErrorCode(ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
