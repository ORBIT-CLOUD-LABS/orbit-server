package com.orbit.server.domain.campaign.model;

import com.orbit.server.global.common.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 관리자가 캠페인을 구분하는 이름.
 *
 * @param value 이름. 공백 불가, 100자 이하
 */
@Embeddable
public record CampaignName(@Column(name = "name") String value) {

    private static final int MAX_LENGTH = 100;

    /**
     * @throws IllegalArgumentException 이름이 비어 있거나 100자를 넘는 경우
     */
    public CampaignName {
        Preconditions.requireText(value, MAX_LENGTH, "캠페인 이름");
    }
}
