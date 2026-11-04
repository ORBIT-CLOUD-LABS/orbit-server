package com.orbit.server.domain.update.entity;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 업데이트 한 번의 결과. 실패했을 때만 실패 정보가 있다.
 *
 * @param status 성공 또는 실패. null 불가
 * @param failure 실패 사유와 상세. 실패면 필수, 성공이면 null
 * @param finishedAt 차량이 업데이트를 마친 시각. null 불가
 * @param downloadEdgeId 차량이 실제로 파일을 받은 Edge 의 id. 매니페스트가 배정한 Edge 가 실패하면 fallbackUrls 의 다른 Edge 일 수 있다.
 *     다운로드 전에 실패했거나 차량이 보내지 않았으면 null. 공백 불가, 50자 이하
 */
@Embeddable
public record UpdateOutcome(
        @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "result")
        UpdateStatus status,

        @Embedded UpdateFailure failure,
        @Column(name = "finished_at") Instant finishedAt,
        @Column(name = "edge_id") String downloadEdgeId) {

    // update_result.edge_id 컬럼 길이. Edge id 는 설정에만 있어 FK 없이 문자열로 저장한다
    private static final int EDGE_ID_MAX_LENGTH = 50;

    /**
     * @throws InvalidValueException 상태나 완료 시각이 없거나, 실패 여부와 실패 정보 유무가 맞지 않거나, 다운로드 Edge 가 공백이거나 50자를
     *     넘는 경우
     */
    public UpdateOutcome {
        if (status == null || finishedAt == null) {
            throw new InvalidValueException("업데이트 결과와 완료 시각은 비어 있을 수 없습니다.");
        }
        // DB 검사 제약(ck_update_result_failure)과 같은 규칙
        if ((status == UpdateStatus.FAILED) != (failure != null)) {
            throw new InvalidValueException("실패한 업데이트에만 실패 정보가 있어야 합니다.");
        }
        // 설정에서 빠진 Edge 의 과거 기록도 남기므로 설정의 Edge 목록과는 대조하지 않는다
        if (downloadEdgeId != null) {
            Preconditions.requireText(downloadEdgeId, EDGE_ID_MAX_LENGTH, "다운로드 Edge id");
        }
    }
}
