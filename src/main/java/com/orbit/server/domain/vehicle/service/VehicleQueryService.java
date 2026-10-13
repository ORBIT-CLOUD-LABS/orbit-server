package com.orbit.server.domain.vehicle.service;

import com.orbit.server.domain.vehicle.entity.VehicleExternalId;
import com.orbit.server.global.error.InvalidValueException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 등록한 차량을 조회한다. 다른 컨텍스트(체크인)는 Vehicle 을 직접 읽지 않고 이 서비스로만 차량 정보를 얻는다. (VEHICLE-REQ-001)
 */
@Service
@RequiredArgsConstructor
public class VehicleQueryService {

    private final VehicleProfileQuery vehicleProfileQuery;

    /**
     * 차량 정보를 조회한다. 차량 토큰 인증을 통과한 요청에서 부른다.
     *
     * @param vehicleId 요청 경로의 차량 식별자
     * @return 차량 ID 와 차종·HW 버전·지역
     * @throws InvalidValueException 식별자가 비어 있거나 64자를 넘는 경우
     * @throws IllegalStateException 등록하지 않은 차량인 경우
     */
    // 토큰은 차량에 딸려 함께 삭제되므로 인증을 통과했는데 차량이 없다면 데이터가 어긋난 것이라 서버 오류로 본다
    public VehicleProfile getProfile(String vehicleId) {
        return vehicleProfileQuery
                .findByExternalId(new VehicleExternalId(vehicleId))
                .orElseThrow(() -> new IllegalStateException("인증한 차량이 등록돼 있지 않습니다: " + vehicleId));
    }
}
