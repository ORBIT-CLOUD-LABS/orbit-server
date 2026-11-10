package com.orbit.server.domain.campaign.entity;

import com.orbit.server.domain.shared.DottedVersion;
import com.orbit.server.global.error.InvalidValueException;
import java.util.Comparator;
import java.util.Objects;

/**
 * 차량이 대상 조건을 만족한 캠페인과 그 목표 버전. 여러 캠페인이 맞을 때 하나를 고르는 데 쓴다.
 *
 * @param campaignId 캠페인 ID. null 불가
 * @param targetVersion 캠페인이 배포하는 파일의 버전. null 불가
 */
public record TargetCampaign(Long campaignId, DottedVersion targetVersion) {

    /** 목표 버전이 높은 캠페인이 앞선다. 같으면 나중에 등록해 ID 가 큰 캠페인이 앞선다. */
    public static final Comparator<TargetCampaign> PRIORITY =
            Comparator.comparing(TargetCampaign::targetVersion).thenComparing(TargetCampaign::campaignId);

    private static final String CURRENT_NAME = "현재 버전";

    public TargetCampaign {
        Objects.requireNonNull(campaignId, "campaignId");
        Objects.requireNonNull(targetVersion, "targetVersion");
    }

    /**
     * 차량의 현재 버전이 목표 버전보다 낮아 이 캠페인으로 올려야 하는지 판정한다. 이미 목표 버전 이상이면 배포하지 않는다.
     *
     * <p>차량은 같은 버전은 설치하지 않고, 목표 버전이 낮으면 다운그레이드로 보고 거부한다. 서버도 같은 숫자 자리 비교로 판정해, 차량이
     * 거부할 파일을 체크인마다 내려줘 다운로드와 실패 보고가 반복되지 않게 한다.
     *
     * @param currentVersion 차량의 현재 버전. 입력 단계에서 형식을 검증해 저장한 값
     * @return 숫자 자리로 비교해 목표 버전보다 낮으면 true
     * @throws InvalidValueException 현재 버전이 점으로 나눈 숫자 형식이 아닌 경우
     */
    public boolean upgrades(String currentVersion) {
        return DottedVersion.of(currentVersion, CURRENT_NAME).compareTo(targetVersion) < 0;
    }
}
