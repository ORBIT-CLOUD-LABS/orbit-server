package com.orbit.server.domain.campaign.exception;

import com.orbit.server.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 캠페인 등록과 매니페스트 발급 중 발생하는 오류. (CAMPAIGN-REQ-001 / CAMPAIGN-POL-001, MANIFEST-REQ-001 /
 * MANIFEST-POL-001)
 *
 * <p>HTTP 상태에 의존하므로 Spring 에 의존하지 않는 entity 가 아닌 exception 에 둔다.
 */
@Getter
public enum CampaignErrorCode implements ErrorCode {
    ACTIVE_CAMPAIGN_EXISTS(HttpStatus.CONFLICT, "같은 대상 조건과 목표 버전의 활성 캠페인이 이미 있습니다."),
    CAMPAIGN_NOT_FOUND(HttpStatus.NOT_FOUND, "캠페인이 없습니다."),
    // 캠페인은 있지만 지금은 배포하지 않는 상태라 없는 캠페인(404)과 구분한다
    CAMPAIGN_INACTIVE(HttpStatus.GONE, "비활성이거나 적용 기간이 아닌 캠페인입니다."),
    NOT_UPDATE_TARGET(HttpStatus.CONFLICT, "이 차량은 캠페인의 업데이트 대상이 아닙니다.");

    private final HttpStatus status;
    private final String message;

    CampaignErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return name();
    }
}
