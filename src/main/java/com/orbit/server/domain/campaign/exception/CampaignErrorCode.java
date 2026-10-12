package com.orbit.server.domain.campaign.exception;

import com.orbit.server.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 캠페인 등록 중 발생하는 오류. (CAMPAIGN-REQ-001 / CAMPAIGN-POL-001)
 *
 * <p>HTTP 상태에 의존하므로 Spring 에 의존하지 않는 entity 가 아닌 exception 에 둔다.
 */
@Getter
public enum CampaignErrorCode implements ErrorCode {
    ACTIVE_CAMPAIGN_EXISTS(HttpStatus.CONFLICT, "같은 대상 조건과 목표 버전의 활성 캠페인이 이미 있습니다.");

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
