package com.orbit.server.domain.campaign.repository;

import com.orbit.server.domain.campaign.entity.Campaign;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 로 구현한 {@link CampaignRepository}.
 *
 * <p>service 는 {@link CampaignRepository} 로만 주입받으므로, {@link JpaRepository} 의 삭제·일괄 저장 같은
 * 메서드는 밖에서 호출할 수 없다.
 */
interface CampaignJpaRepository extends JpaRepository<Campaign, Long>, CampaignRepository {

    // idx_campaign_status_model 인덱스를 타도록 상태와 차종으로만 거른다
    @Override
    @Query("""
            select c from Campaign c
            where c.status = com.orbit.server.domain.campaign.entity.CampaignStatus.ACTIVE
              and c.condition.target.model = :model
            """)
    List<Campaign> findAllActiveByModel(@Param("model") String model);
}
