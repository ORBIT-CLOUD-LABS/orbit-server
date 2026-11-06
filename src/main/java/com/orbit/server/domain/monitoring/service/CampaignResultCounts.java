package com.orbit.server.domain.monitoring.service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 한 캠페인의 결과·실패 사유별 차량 수. 성공·실패 합계와 사유별 실패 수를 계산한다.
 *
 * <p>대시보드가 사유마다 값이 있는지 확인하지 않도록 보고된 적 없는 사유도 0 으로 채운다.
 */
public class CampaignResultCounts {

    private static final String SUCCEEDED = "SUCCEEDED";
    private static final String FAILED = "FAILED";

    private final List<CampaignResultCount> counts;

    /**
     * @param counts 결과·실패 사유별 차량 수. 없으면 빈 목록
     */
    public CampaignResultCounts(List<CampaignResultCount> counts) {
        this.counts = List.copyOf(counts);
    }

    /**
     * @return 업데이트에 성공한 차량 수
     */
    public long succeeded() {
        return sumOf(SUCCEEDED);
    }

    /**
     * @return 업데이트에 실패한 차량 수. 사유와 상관없이 모두 더한다
     */
    public long failed() {
        return sumOf(FAILED);
    }

    /**
     * 실패 사유별 차량 수를 센다. 모르는 사유는 사유별 수에서 빠지지만 {@link #failed()} 에는 들어간다.
     *
     * @return 모든 실패 사유와 그 차량 수. 사유 선언 순서이고 수정할 수 없다
     */
    public Map<UpdateFailureReason, Long> failureReasons() {
        Map<UpdateFailureReason, Long> reasons = new EnumMap<>(UpdateFailureReason.class);
        for (UpdateFailureReason reason : UpdateFailureReason.values()) {
            reasons.put(reason, sumOfFailure(reason));
        }
        return Collections.unmodifiableMap(reasons);
    }

    private long sumOf(String result) {
        return counts.stream()
                .filter(count -> count.result().equals(result))
                .mapToLong(CampaignResultCount::count)
                .sum();
    }

    private long sumOfFailure(UpdateFailureReason reason) {
        return counts.stream()
                .filter(count -> count.result().equals(FAILED))
                .filter(count -> reason.name().equals(count.failureReason()))
                .mapToLong(CampaignResultCount::count)
                .sum();
    }
}
