package com.orbit.server.domain.update.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 업데이트 한 번의 결과. 실패했을 때만 실패 정보가 있다.
 *
 * @param status 성공 또는 실패. null 불가
 * @param failure 실패 사유와 상세. 실패면 필수, 성공이면 null
 * @param finishedAt 차량이 업데이트를 마친 시각. null 불가
 */
@Embeddable
public record UpdateOutcome(
        @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "result")
        UpdateStatus status,

        @Embedded UpdateFailure failure,
        @Column(name = "finished_at") LocalDateTime finishedAt) {

    /**
     * @throws IllegalArgumentException 상태나 완료 시각이 없거나, 실패 여부와 실패 정보 유무가 맞지 않는 경우
     */
    public UpdateOutcome {
        if (status == null || finishedAt == null) {
            throw new IllegalArgumentException("업데이트 결과와 완료 시각은 비어 있을 수 없습니다.");
        }
        // DB 검사 제약(ck_update_result_failure)과 같은 규칙
        if ((status == UpdateStatus.FAILED) != (failure != null)) {
            throw new IllegalArgumentException("실패한 업데이트에만 실패 정보가 있어야 합니다.");
        }
    }
}
