package com.orbit.server.domain.campaign.entity;

import java.util.Comparator;
import java.util.Objects;

/**
 * 차량이 대상 조건을 만족한 캠페인과 그 목표 버전. 여러 캠페인이 맞을 때 하나를 고르는 데 쓴다. (VEHICLE-POL-001)
 *
 * @param campaignId 캠페인 ID. null 불가
 * @param targetVersion 캠페인이 배포하는 파일의 버전. null 불가
 */
public record TargetCampaign(Long campaignId, DottedVersion targetVersion) {

    /** 목표 버전이 높은 캠페인이 앞선다. 같으면 나중에 등록해 ID 가 큰 캠페인이 앞선다. */
    public static final Comparator<TargetCampaign> PRIORITY =
            Comparator.comparing(TargetCampaign::targetVersion).thenComparing(TargetCampaign::campaignId);

    public TargetCampaign {
        Objects.requireNonNull(campaignId, "campaignId");
        Objects.requireNonNull(targetVersion, "targetVersion");
    }

    /**
     * 차량의 현재 버전이 이미 목표 버전인지 판정한다. 이미 설치한 버전은 다시 배포하지 않는다. (VEHICLE-POL-001)
     *
     * @param currentVersion 차량의 현재 버전
     * @return 숫자 자리로 비교해 목표 버전과 같으면 true
     */
    public boolean isInstalledOn(String currentVersion) {
        return DottedVersion.parse(currentVersion).filter(targetVersion::equals).isPresent();
    }
}
