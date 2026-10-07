package com.orbit.server.domain.campaign.model;

import com.orbit.server.global.common.Preconditions;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import java.util.HashSet;
import java.util.Set;

/**
 * 캠페인 대상 차량의 차종·HW 버전·지역.
 *
 * <p>HW 버전과 지역은 비어 있으면 전체가 대상이다. 행이 없는 것으로 "전체"를 표현하므로 별도 플래그를 두지 않는다.
 */
@Embeddable
public class CampaignTarget {

    private static final int MAX_LENGTH = 50;

    @Column(name = "model")
    private String model;

    @ElementCollection
    @CollectionTable(name = "campaign_target_hw_version", joinColumns = @JoinColumn(name = "campaign_id"))
    @Column(name = "hw_version")
    private Set<String> hwVersions = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "campaign_target_region", joinColumns = @JoinColumn(name = "campaign_id"))
    @Column(name = "region")
    private Set<String> regions = new HashSet<>();

    protected CampaignTarget() {}

    /**
     * @param model 대상 차종. 공백 불가, 50자 이하
     * @param hwVersions 대상 HW 버전. null 이거나 비어 있으면 전체
     * @param regions 대상 지역. null 이거나 비어 있으면 전체
     * @throws IllegalArgumentException 차종이나 HW 버전·지역 값이 비어 있거나 50자를 넘는 경우
     */
    public CampaignTarget(String model, Set<String> hwVersions, Set<String> regions) {
        this.model = Preconditions.requireText(model, MAX_LENGTH, "대상 차종");
        this.hwVersions = copyOf(hwVersions, "대상 HW 버전");
        this.regions = copyOf(regions, "대상 지역");
    }

    // JPA 가 컬렉션을 감싸 관리하므로 불변 컬렉션이 아닌 복사본을 둔다
    private static Set<String> copyOf(Set<String> values, String name) {
        Set<String> copied = new HashSet<>();
        if (values == null) {
            return copied;
        }
        values.forEach(value -> copied.add(Preconditions.requireText(value, MAX_LENGTH, name)));
        return copied;
    }
}
