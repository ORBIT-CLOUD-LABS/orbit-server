package com.orbit.server.domain.update.service;

import com.orbit.server.domain.update.entity.FailureReason;
import com.orbit.server.domain.update.entity.UpdateFailure;
import com.orbit.server.domain.update.entity.UpdateOutcome;
import com.orbit.server.domain.update.entity.UpdateStatus;
import com.orbit.server.global.error.InvalidValueException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * 차량이 체크인에 담아 보낸 업데이트 결과({@code lastUpdate}). (UPDATE-REQ-001)
 *
 * <p>차량은 체크인이 성공할 때까지 같은 결과를 다시 보낸다. 형식이 틀린 결과 때문에 체크인 요청이 거부되면 그 차량의 체크인이 계속
 * 실패하므로, 요청을 읽을 때는 검증하지 않고 원문 그대로 받아 {@link #toOutcome()} 에서 한 번에 검증한다.
 *
 * @param campaignId 결과가 속한 캠페인 ID
 * @param result {@code SUCCEEDED} 또는 {@code FAILED}
 * @param failureReason 실패 사유. 실패일 때만 있다
 * @param failureDetail 실패 상세. 실패 사유가 있을 때만 보낼 수 있다
 * @param finishedAt 차량이 업데이트를 마친 시각 (ISO-8601)
 */
public record ReportUpdateResultCommand(
        Long campaignId, String result, String failureReason, String failureDetail, String finishedAt) {

    /**
     * 보고 전체를 검증하고 업데이트 결과로 바꾼다. 실패 여부와 실패 사유 유무가 맞는지는 {@link UpdateOutcome} 이 검증한다.
     * (UPDATE-POL-001)
     *
     * @return 성공·실패와 실패 정보, 완료 시각
     * @throws InvalidValueException 캠페인 ID·결과·완료 시각이 없거나 형식이 틀린 경우, 정의하지 않은 결과·실패 사유인 경우, 실패
     *     여부와 실패 사유 유무가 맞지 않는 경우, 실패 사유 없이 실패 상세가 있는 경우, 실패 상세가 500자를 넘는 경우
     */
    public UpdateOutcome toOutcome() {
        if (campaignId == null) {
            throw new InvalidValueException("업데이트 결과의 캠페인 ID 는 비어 있을 수 없습니다.");
        }
        return new UpdateOutcome(parse(UpdateStatus.class, result, "업데이트 결과"), toFailure(), parseFinishedAt());
    }

    // 상세가 없을 때 null 대신 빈 문자열을 보내는 차량도 있을 수 있으므로 공백은 보내지 않은 것으로 본다
    private UpdateFailure toFailure() {
        if (isBlank(failureReason)) {
            requireNoDetail();
            return null;
        }
        return new UpdateFailure(parse(FailureReason.class, failureReason, "실패 사유"), failureDetail);
    }

    // 실패 사유 없이 온 상세는 저장할 곳이 없어 조용히 사라지므로 거부한다
    private void requireNoDetail() {
        if (!isBlank(failureDetail)) {
            throw new InvalidValueException("실패 사유 없이 실패 상세를 보낼 수 없습니다.");
        }
    }

    private Instant parseFinishedAt() {
        if (finishedAt == null) {
            throw new InvalidValueException("업데이트 완료 시각은 비어 있을 수 없습니다.");
        }
        try {
            return Instant.parse(finishedAt);
        } catch (DateTimeParseException e) {
            throw new InvalidValueException("업데이트 완료 시각은 ISO-8601 형식이어야 합니다: " + finishedAt, e);
        }
    }

    // valueOf 는 정의하지 않은 값에 IllegalArgumentException 을 던지므로 이름으로 찾아 입력 오류로 바꾼다
    private static <E extends Enum<E>> E parse(Class<E> type, String value, String name) {
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new InvalidValueException(name + " 값이 올바르지 않습니다: " + value));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
