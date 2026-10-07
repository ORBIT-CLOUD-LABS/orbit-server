package com.orbit.server.domain.vehicle.model;

/** 차량 토큰 상태. 폐기한 토큰으로는 인증할 수 없다. */
public enum CredentialStatus {
    ACTIVE,
    REVOKED
}
