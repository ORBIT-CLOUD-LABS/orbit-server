package com.orbit.server.domain.edge.entity;

import com.orbit.server.global.error.InvalidValueException;

/**
 * Edge 하나의 헬스체크 결과. 연속 실패 횟수만 기억하고, 정상 여부는 실패 기준 횟수와 비교해 판정한다.
 *
 * <p>한 번의 실패로 빼면 순간적인 네트워크 오류에도 Edge 가 선택에서 빠졌다 돌아오기를 반복하므로, 연속으로 실패해야 뺀다. 반대로 한 번이라도
 * 성공하면 바로 되돌려 복구된 Edge 를 빨리 다시 쓴다.
 *
 * @param consecutiveFailures 마지막 성공 이후 연속으로 실패한 횟수. 0 이상
 */
public record EdgeHealth(int consecutiveFailures) {

    private static final EdgeHealth INITIAL = new EdgeHealth(0);

    /**
     * @throws InvalidValueException 연속 실패 횟수가 음수인 경우
     */
    public EdgeHealth {
        if (consecutiveFailures < 0) {
            throw new InvalidValueException("연속 실패 횟수는 음수일 수 없습니다.");
        }
    }

    /**
     * 첫 확인 전의 상태. 새 Pod 가 뜨자마자 모든 Edge 를 빼면 첫 확인까지 매니페스트를 발급하지 못하므로 정상으로 시작한다.
     *
     * @return 연속 실패가 없는 상태
     */
    public static EdgeHealth initial() {
        return INITIAL;
    }

    /**
     * @return 헬스체크에 성공한 뒤의 상태. 연속 실패가 끊긴다
     */
    public EdgeHealth success() {
        return INITIAL;
    }

    /**
     * @return 헬스체크에 실패한 뒤의 상태
     */
    public EdgeHealth failure() {
        // 실패가 아주 오래 이어져도 음수로 넘어가지 않도록 최댓값에서 멈춘다
        if (consecutiveFailures == Integer.MAX_VALUE) {
            return this;
        }
        return new EdgeHealth(consecutiveFailures + 1);
    }

    /**
     * @param failureThreshold 비정상으로 볼 연속 실패 횟수
     * @return 연속 실패가 기준 횟수보다 적으면 true
     */
    public boolean isHealthy(int failureThreshold) {
        return consecutiveFailures < failureThreshold;
    }
}
