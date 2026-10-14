package com.orbit.server.domain.campaign.repository;

import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.service.CampaignFilter;
import com.orbit.server.domain.campaign.service.CampaignSummary;
import com.orbit.server.domain.campaign.service.CampaignSummaryQuery;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 로 구현한 {@link CampaignSummaryQuery}.
 *
 * <p>Campaign 에 getter 를 두지 않으므로 엔티티를 읽지 않고 JPQL 생성자 표현식으로 바로 {@link CampaignSummary} 를 만든다.
 * 같은 밀리초에 등록된 캠페인도 순서가 정해지도록 ID 를 두 번째 정렬 기준으로 쓴다.
 */
interface CampaignSummaryJpaQuery extends Repository<Campaign, Long>, CampaignSummaryQuery {

    // 필터 조합마다 메서드를 나누지 않도록 값이 없는 조건은 is null 로 건너뛴다
    @Override
    @Query("""
            select new com.orbit.server.domain.campaign.service.CampaignSummary(
                c.id, c.artifactId, c.name.value, c.condition.target.model, c.status,
                c.condition.period.startAt, c.condition.period.endAt, c.createdAt)
            from Campaign c
            where (:#{#filter.status} is null or c.status = :#{#filter.status})
              and (:#{#filter.model} is null or c.condition.target.model = :#{#filter.model})
            order by c.createdAt desc, c.id desc
            """)
    List<CampaignSummary> findAll(@Param("filter") CampaignFilter filter);
}
