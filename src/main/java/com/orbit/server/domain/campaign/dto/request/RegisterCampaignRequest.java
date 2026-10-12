package com.orbit.server.domain.campaign.dto.request;

import com.orbit.server.domain.campaign.entity.CampaignName;
import com.orbit.server.domain.campaign.entity.CampaignPeriod;
import com.orbit.server.domain.campaign.entity.TargetRelease;
import com.orbit.server.domain.campaign.entity.VersionRange;
import com.orbit.server.domain.campaign.service.RegisterCampaignCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;

/**
 * 캠페인 등록 요청.
 *
 * @param name 캠페인 이름
 * @param artifactId 배포할 Origin 파일 ID
 * @param targetVersion 목표 버전. 고른 파일의 버전과 같아야 한다
 * @param target 대상 차량 조건
 * @param startAt 적용 시작 시각
 * @param endAt 적용 종료 시각. 시작 시각보다 늦어야 한다
 */
public record RegisterCampaignRequest(
        @NotBlank String name,
        @NotNull Long artifactId,
        @NotBlank String targetVersion,
        @NotNull @Valid Target target,
        @NotNull Instant startAt,
        @NotNull Instant endAt) {

    /**
     * 캠페인 대상 차량 조건.
     *
     * @param model 대상 차종. 고른 파일의 차종과 같아야 한다
     * @param hwVersions 대상 HW 버전. 생략하거나 비어 있으면 전체
     * @param regions 대상 지역. 생략하거나 비어 있으면 전체
     * @param currentVersionMin 대상 차량의 최소 현재 버전(포함). 생략하면 제한 없음
     * @param currentVersionMax 대상 차량의 최대 현재 버전(포함). 생략하면 제한 없음
     */
    public record Target(
            @NotBlank String model,
            Set<String> hwVersions,
            Set<String> regions,
            String currentVersionMin,
            String currentVersionMax) {}

    public RegisterCampaignCommand toCommand() {
        return new RegisterCampaignCommand(
                new CampaignName(name),
                artifactId,
                new TargetRelease(target.model(), targetVersion),
                target.hwVersions(),
                target.regions(),
                new VersionRange(target.currentVersionMin(), target.currentVersionMax()),
                new CampaignPeriod(startAt, endAt));
    }
}
