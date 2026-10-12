package com.orbit.server.global.security;

import com.orbit.server.global.error.BusinessException;

/**
 * 차량 API 요청의 토큰을 확인한다. (VEHICLE-POL-003)
 *
 * <p>토큰 저장소는 차량 도메인에 있고 {@code global} 은 {@code domain} 에 의존하지 않으므로, 인터셉터는 이 포트만 알고 구현은
 * 차량 도메인이 제공한다.
 */
public interface VehicleTokenAuthenticator {

    /**
     * 토큰이 유효하고 경로의 차량에 발급된 것인지 확인한다.
     *
     * @param token {@code Authorization: Bearer} 뒤의 토큰 원문
     * @param vehicleId 요청 경로의 차량 식별자
     * @throws BusinessException 토큰이 없거나 폐기됐으면 UNAUTHORIZED, 다른 차량의 토큰이면 VEHICLE_MISMATCH
     */
    void authenticate(String token, String vehicleId);
}
