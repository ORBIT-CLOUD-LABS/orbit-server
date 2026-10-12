package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.entity.CampaignCondition;
import com.orbit.server.domain.campaign.entity.CampaignName;
import com.orbit.server.domain.campaign.entity.CampaignPeriod;
import com.orbit.server.domain.campaign.entity.CampaignTarget;
import com.orbit.server.domain.campaign.entity.TargetRelease;
import com.orbit.server.domain.campaign.entity.VersionRange;
import java.util.Objects;
import java.util.Set;

/**
 * 캠페인 등록 요청.
 *
 * @param name 캠페인 이름
 * @param artifactId 배포할 Artifact ID
 * @param release 대상 차종과 목표 버전. 고른 파일과 같은지 검증한다
 * @param hwVersions 대상 HW 버전. null 이거나 비어 있으면 전체
 * @param regions 대상 지역. null 이거나 비어 있으면 전체
 * @param currentVersionRange 대상 차량의 현재 버전 범위
 * @param period 적용 기간
 */
public record RegisterCampaignCommand(
        CampaignName name,
        Long artifactId,
        TargetRelease release,
        Set<String> hwVersions,
        Set<String> regions,
        VersionRange currentVersionRange,
        CampaignPeriod period) {

    public RegisterCampaignCommand {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(artifactId, "artifactId");
        Objects.requireNonNull(release, "release");
    }

    /**
     * @return 등록 즉시 활성 상태인 캠페인. 목표 버전은 파일 버전을 따르므로 담지 않는다
     */
    public Campaign toCampaign() {
        CampaignTarget target = new CampaignTarget(release.model(), hwVersions, regions);
        return new Campaign(name, artifactId, new CampaignCondition(target, currentVersionRange, period));
    }
}
