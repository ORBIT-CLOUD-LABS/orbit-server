package com.orbit.server.domain.campaign.repository;

import com.orbit.server.domain.campaign.entity.Campaign;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 등록한 캠페인 저장소.
 *
 * <p>{@link #findById} 와 {@link #existsById} 는 활성 여부를 보지 않는다. 비활성 캠페인도 결과 기록과 상세 조회에는 필요하기 때문이다.
 */
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    /**
     * 이 차종을 대상으로 하는 활성 캠페인을 조회한다. 중복 판정과 체크인 대상 판정에서 후보를 좁힐 때 쓴다.
     *
     * <p>HW 버전·지역·버전 범위는 여러 테이블에 걸치거나 문자열 비교가 틀리므로 DB 에서 거르지 않고, 차종으로만 좁힌 뒤 도메인에서 판정한다.
     *
     * @param model 대상 차종
     * @return 이 차종의 활성 캠페인. 없으면 빈 목록
     */
    // idx_campaign_status_model 인덱스를 타도록 상태와 차종으로만 거른다
    @Query("""
            select c from Campaign c
            where c.status = com.orbit.server.domain.campaign.entity.CampaignStatus.ACTIVE
              and c.condition.target.model = :model
            """)
    List<Campaign> findAllActiveByModel(@Param("model") String model);
}
