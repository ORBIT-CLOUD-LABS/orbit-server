package com.orbit.server.domain.update.repository;

import com.orbit.server.domain.update.entity.UpdateOutcome;

/** 차량이 보고한 업데이트 결과 저장소. 차량·캠페인당 결과를 하나만 둔다. */
public interface UpdateResultRepository {

    /**
     * 업데이트 결과를 기록한다. 같은 차량·캠페인의 결과가 이미 있으면 덮어쓰므로 같은 보고가 다시 와도 결과는 같다. (UPDATE-POL-001)
     *
     * @param vehicleId 결과를 보고한 차량 ID
     * @param campaignId 결과가 속한 캠페인 ID. 존재하는 캠페인이어야 한다
     * @param outcome 성공·실패와 실패 사유, 완료 시각
     */
    void record(Long vehicleId, Long campaignId, UpdateOutcome outcome);
}
