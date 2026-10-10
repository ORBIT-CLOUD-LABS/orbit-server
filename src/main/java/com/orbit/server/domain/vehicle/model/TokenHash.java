package com.orbit.server.domain.vehicle.model;

import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 차량 토큰 원문의 SHA-256 해시. DB 가 유출돼도 토큰을 복원할 수 없도록 원문 대신 저장한다.
 *
 * @param value 소문자 16진수 64자
 */
@Embeddable
public record TokenHash(
        @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "token_hash", length = 64)
        String value) {

    // DB 검사 제약(ck_vehicle_credential_token_hash)과 같은 형식
    private static final Pattern FORMAT = Pattern.compile("^[0-9a-f]{64}$");

    /**
     * @throws InvalidValueException 소문자 16진수 64자가 아닌 경우
     */
    public TokenHash {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("토큰 해시는 소문자 16진수 64자여야 합니다.");
        }
    }
}
