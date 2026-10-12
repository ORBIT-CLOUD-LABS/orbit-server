package com.orbit.server.domain.vehicle.service;

import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.domain.vehicle.entity.VehicleToken;
import com.orbit.server.domain.vehicle.exception.VehicleErrorCode;
import com.orbit.server.domain.vehicle.repository.VehicleCredentialRepository;
import com.orbit.server.global.error.BusinessException;
import com.orbit.server.global.error.InvalidValueException;
import com.orbit.server.global.security.AuthErrorCode;
import com.orbit.server.global.security.VehicleTokenAuthenticator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차량 API 요청의 토큰이 경로 차량에 발급한 유효 토큰인지 확인한다. (VEHICLE-POL-003)
 *
 * <p>인터셉터가 {@link VehicleTokenAuthenticator} 로 호출한다.
 */
@Service
@RequiredArgsConstructor
public class VehicleAuthenticationService implements VehicleTokenAuthenticator {

    private final VehicleCredentialRepository vehicleCredentialRepository;

    /**
     * 토큰 해시로 유효 토큰의 차량을 찾아 경로 차량과 비교한다.
     *
     * @throws BusinessException 토큰 형식이 아니거나 없거나 폐기됐으면 UNAUTHORIZED, 다른 차량의 토큰이면 VEHICLE_MISMATCH
     * @throws InvalidValueException 경로의 차량 식별자가 비어 있거나 64자를 넘는 경우
     */
    @Override
    @Transactional(readOnly = true)
    public void authenticate(String token, String vehicleId) {
        VehicleExternalId owner = vehicleCredentialRepository
                .findActiveVehicleExternalId(toToken(token).hash())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.UNAUTHORIZED));
        if (!owner.equals(new VehicleExternalId(vehicleId))) {
            throw new BusinessException(VehicleErrorCode.VEHICLE_MISMATCH);
        }
    }

    // 형식이 틀린 토큰도 발급한 적 없는 토큰이므로 입력 오류(400)가 아니라 인증 실패로 응답해 차량이 재등록하게 한다
    private VehicleToken toToken(String token) {
        try {
            return new VehicleToken(token);
        } catch (InvalidValueException e) {
            throw new BusinessException(AuthErrorCode.UNAUTHORIZED, e);
        }
    }
}
