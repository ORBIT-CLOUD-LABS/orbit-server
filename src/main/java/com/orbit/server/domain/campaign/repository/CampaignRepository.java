package com.orbit.server.domain.campaign.repository;

import com.orbit.server.domain.campaign.entity.Campaign;
import com.orbit.server.domain.campaign.service.CampaignFilter;
import com.orbit.server.domain.campaign.service.CampaignSummary;
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

    /**
     * 관리자가 보는 캠페인 목록을 조회한다. 목록은 필요한 컬럼만 읽도록 엔티티 대신 JPQL 생성자 표현식으로 바로
     * {@link CampaignSummary} 를 만든다.
     *
     * <p>캠페인은 관리자가 직접 등록하는 만큼만 쌓이므로 페이지 없이 전체를 돌려준다. 같은 밀리초에 등록된 캠페인도 순서가 정해지도록
     * ID 를 두 번째 정렬 기준으로 쓴다.
     *
     * @param filter 상태·차종 필터
     * @return 필터에 맞는 캠페인. 최근 등록한 순서. 없으면 빈 목록
     */
    // 필터 조합마다 메서드를 나누지 않도록 값이 없는 조건은 is null 로 건너뛴다
    @Query("""
            select new com.orbit.server.domain.campaign.service.CampaignSummary(
                c.id, c.artifactId, c.name.value, c.condition.target.model, c.status,
                c.condition.period.startAt, c.condition.period.endAt, c.createdAt)
            from Campaign c
            where (:#{#filter.status} is null or c.status = :#{#filter.status})
              and (:#{#filter.model} is null or c.condition.target.model = :#{#filter.model})
            order by c.createdAt desc, c.id desc
            """)
    List<CampaignSummary> findAllSummaries(@Param("filter") CampaignFilter filter);
}
