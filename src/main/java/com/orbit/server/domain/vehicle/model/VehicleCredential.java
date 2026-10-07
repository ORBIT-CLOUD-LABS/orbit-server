package com.orbit.server.domain.vehicle.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 차량 인증 토큰. 원문은 발급 응답에서 한 번만 내려주고, 여기에는 해시만 저장한다.
 *
 * <p>재등록 시 기존 토큰을 폐기하고 새로 발급하므로 이력이 쌓인다. 차량당 유효 토큰이 하나뿐인 것은 DB 제약이 보장한다.
 */
@Entity
@Table(name = "vehicle_credential")
public class VehicleCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Embedded
    private TokenHash tokenHash;

    // MySQL 방언은 enum 을 네이티브 ENUM 타입으로 매핑하므로 VARCHAR 컬럼에 맞춘다
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status")
    private CredentialStatus status;

    @Generated
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    protected VehicleCredential() {}

    /**
     * 차량에 새 토큰을 발급한다. 발급한 토큰은 유효 상태다.
     *
     * @param vehicleId 토큰을 발급할 차량 ID. null 불가
     * @param tokenHash 토큰 원문의 SHA-256 해시. null 불가
     */
    public VehicleCredential(Long vehicleId, TokenHash tokenHash) {
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicleId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.status = CredentialStatus.ACTIVE;
    }
}
