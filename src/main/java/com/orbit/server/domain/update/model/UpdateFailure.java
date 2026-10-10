package com.orbit.server.domain.update.model;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 업데이트 실패 정보. 캠페인 진행 현황에서 실패 사유별로 집계한다.
 *
 * @param reason 실패 사유. null 불가
 * @param detail 차량이 보낸 상세 내용. null 가능, 500자 이하
 */
@Embeddable
public record UpdateFailure(
        @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "failure_reason")
        FailureReason reason,

        @Column(name = "failure_detail") String detail) {

    private static final int DETAIL_MAX_LENGTH = 500;

    /**
     * @throws InvalidValueException 실패 사유가 없거나 상세 내용이 500자를 넘는 경우
     */
    public UpdateFailure {
        if (reason == null) {
            throw new InvalidValueException("실패 사유는 비어 있을 수 없습니다.");
        }
        Preconditions.requireMaxLength(detail, DETAIL_MAX_LENGTH, "실패 상세");
    }
}
