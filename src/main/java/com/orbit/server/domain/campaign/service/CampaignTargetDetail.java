package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.campaign.entity.CampaignCondition;
import com.orbit.server.domain.campaign.entity.CampaignTarget;
import com.orbit.server.domain.campaign.entity.VersionRange;
import java.util.Collection;
import java.util.List;

/**
 * 캠페인 상세의 대상 조건.
 *
 * @param model 대상 차종
 * @param hwVersions 대상 HW 버전. 이름 순서. 비어 있으면 전체
 * @param regions 대상 지역. 이름 순서. 비어 있으면 전체
 * @param currentVersionMin 대상 차량의 최소 현재 버전(포함). null 이면 제한 없음
 * @param currentVersionMax 대상 차량의 최대 현재 버전(포함). null 이면 제한 없음
 */
public record CampaignTargetDetail(
        String model,
        List<String> hwVersions,
        List<String> regions,
        String currentVersionMin,
        String currentVersionMax) {

    public CampaignTargetDetail {
        hwVersions = List.copyOf(hwVersions);
        regions = List.copyOf(regions);
    }

    /**
     * HW 버전·지역을 지연 로딩하므로 트랜잭션 안에서 부른다.
     *
     * @param condition 캠페인 대상 조건
     * @return 대상 조건. HW 버전·지역은 응답 순서가 요청마다 바뀌지 않도록 이름 순서로 담는다
     */
    static CampaignTargetDetail of(CampaignCondition condition) {
        CampaignTarget target = condition.getTarget();
        VersionRange range = condition.getCurrentVersionRange();
        return new CampaignTargetDetail(
                target.getModel(),
                sorted(target.getHwVersions()),
                sorted(target.getRegions()),
                range.min(),
                range.max());
    }

    private static List<String> sorted(Collection<String> values) {
        return values.stream().sorted().toList();
    }
}
