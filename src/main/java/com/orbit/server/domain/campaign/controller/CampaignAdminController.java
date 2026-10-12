package com.orbit.server.domain.campaign.controller;

import com.orbit.server.domain.campaign.dto.request.RegisterCampaignRequest;
import com.orbit.server.domain.campaign.dto.response.CampaignRegistrationResponse;
import com.orbit.server.domain.campaign.service.CampaignRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자용 캠페인 API. */
@RestController
@RequestMapping("/api/v1/admin/campaigns")
@RequiredArgsConstructor
public class CampaignAdminController {

    private final CampaignRegistrationService campaignRegistrationService;

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
}
