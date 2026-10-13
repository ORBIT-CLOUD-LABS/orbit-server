package com.orbit.server.domain.vehicle.service;

/**
 * 체크인에서 쓰는 차량 정보. 상태를 기록할 차량 ID 와 캠페인 대상 판정에 쓸 차종·HW 버전·지역을 담는다. (VEHICLE-REQ-001)
 *
 * <p>조회 전용이라 Vehicle 에 getter 를 두지 않고 DB 에서 바로 이 형태로 읽는다.
 *
 * @param vehicleId 차량 ID
 * @param model 차종
 * @param hwVersion HW 버전
 * @param region 지역
 */
public record VehicleProfile(Long vehicleId, String model, String hwVersion, String region) {}
