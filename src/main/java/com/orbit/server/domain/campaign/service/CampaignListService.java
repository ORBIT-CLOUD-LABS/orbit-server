package com.orbit.server.domain.campaign.service;

import com.orbit.server.domain.artifact.service.ArtifactQueryService;
import com.orbit.server.domain.artifact.service.ArtifactSummary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 관리자가 보는 캠페인 목록을 조회한다. (#18)
 *
 * <p>목표 버전은 Artifact 에 있으므로 다른 컨텍스트인 Artifact 를 {@link ArtifactQueryService} 로 읽어 붙인다. 캠페인마다 따로
 * 조회하지 않도록 목록에 나온 Artifact 를 한 번에 읽는다.
 */
@Service
@RequiredArgsConstructor
public class CampaignListService {

    private final CampaignSummaryQuery campaignSummaryQuery;
    private final ArtifactQueryService artifactQueryService;

    /**
     * @param filter 상태·차종 필터
     * @return 필터에 맞는 캠페인과 목표 버전. 최근 등록한 순서. 없으면 빈 목록
     */
    public List<CampaignListItem> findAll(CampaignFilter filter) {
        List<CampaignSummary> campaigns = campaignSummaryQuery.findAll(filter);
        Map<Long, String> targetVersions = targetVersionsOf(campaigns);
        return campaigns.stream()
                .map(campaign -> new CampaignListItem(campaign, targetVersions.get(campaign.artifactId())))
                .toList();
    }

    // campaign.artifact_id 는 외래 키라 Artifact 가 빠지는 일은 없다
    private Map<Long, String> targetVersionsOf(List<CampaignSummary> campaigns) {
        Set<Long> artifactIds =
                campaigns.stream().map(CampaignSummary::artifactId).collect(Collectors.toSet());
        return artifactQueryService.findAllByIds(artifactIds).stream()
                .collect(Collectors.toMap(ArtifactSummary::artifactId, ArtifactSummary::version));
    }
}
