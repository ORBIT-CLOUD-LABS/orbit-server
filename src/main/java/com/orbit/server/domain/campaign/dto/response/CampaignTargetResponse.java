package com.orbit.server.domain.campaign.dto.response;

import com.orbit.server.domain.campaign.service.CampaignTargetDetail;
import java.util.List;

/**
 * 캠페인 대상 조건. 등록 요청의 {@code target} 과 같은 모양이다.
 *
 * @param model 대상 차종
 * @param hwVersions 대상 HW 버전. 이름 순서. 비어 있으면 전체
 * @param regions 대상 지역. 이름 순서. 비어 있으면 전체
 * @param currentVersionMin 대상 차량의 최소 현재 버전(포함). 없으면 제한 없음
 * @param currentVersionMax 대상 차량의 최대 현재 버전(포함). 없으면 제한 없음
 */
public record CampaignTargetResponse(
        String model,
        List<String> hwVersions,
        List<String> regions,
        String currentVersionMin,
        String currentVersionMax) {

    static CampaignTargetResponse from(CampaignTargetDetail target) {
        return new CampaignTargetResponse(
                target.model(),
                target.hwVersions(),
                target.regions(),
                target.currentVersionMin(),
                target.currentVersionMax());
    }
}
