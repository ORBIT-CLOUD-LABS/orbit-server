package com.orbit.server.domain.campaign.controller;

import com.orbit.server.domain.campaign.dto.request.RegisterCampaignRequest;
import com.orbit.server.domain.campaign.dto.response.CampaignListResponse;
import com.orbit.server.domain.campaign.dto.response.CampaignRegistrationResponse;
import com.orbit.server.domain.campaign.entity.CampaignStatus;
import com.orbit.server.domain.campaign.service.CampaignFilter;
import com.orbit.server.domain.campaign.service.CampaignListService;
import com.orbit.server.domain.campaign.service.CampaignRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자용 캠페인 API. */
@RestController
@RequestMapping("/api/v1/admin/campaigns")
@RequiredArgsConstructor
public class CampaignAdminController {

    private final CampaignRegistrationService campaignRegistrationService;
    private final CampaignListService campaignListService;

    /**
     * 대상 조건·목표 버전·기간으로 캠페인을 등록한다. 등록 즉시 활성 상태가 된다. (CAMPAIGN-REQ-001 / CAMPAIGN-POL-001)
     *
     * @param request 이름, 파일 ID, 목표 버전, 대상 조건, 적용 기간
     * @return 201 과 등록한 캠페인 ID
     */
    @PostMapping
    public ResponseEntity<CampaignRegistrationResponse> register(@Valid @RequestBody RegisterCampaignRequest request) {
        Long campaignId = campaignRegistrationService.register(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CampaignRegistrationResponse(campaignId));
    }

    /**
     * 등록한 캠페인 목록을 상태·차종으로 필터링해 조회한다. 목표 버전은 캠페인이 배포하는 Artifact 의 버전이다.
     *
     * @param status 캠페인 상태. 선택이며, 없으면 모든 상태. 정의되지 않은 값이면 400 INVALID_INPUT
     * @param model 대상 차종. 선택이며, 없으면 모든 차종
     * @return 200 과 최근 등록한 순서의 캠페인 목록
     */
    @GetMapping
    public CampaignListResponse findAll(
            @RequestParam(required = false) CampaignStatus status, @RequestParam(required = false) String model) {
        return CampaignListResponse.from(campaignListService.findAll(new CampaignFilter(status, model)));
    }
}
