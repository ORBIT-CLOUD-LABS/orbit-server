package com.orbit.server.domain.update.controller;

import com.orbit.server.domain.update.dto.response.ManifestResponse;
import com.orbit.server.domain.update.service.ManifestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 체크인에서 업데이트 대상으로 판정받은 차량이 호출하는 매니페스트 API. */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class ManifestController {

    private final ManifestService manifestService;

    /**
     * 요청 시점에 대상 여부를 다시 확인하고 파일 정보와 CDN 다운로드 URL 을 응답한다. (MANIFEST-REQ-001 / MANIFEST-POL-001)
     *
     * <p>차량 토큰과 경로 {@code vehicleId} 확인은 이 경로에 건 인터셉터가 먼저 한다.
     *
     * @param vehicleId 차량 식별자
     * @param campaignId 체크인에서 받은 캠페인 ID
     * @return 캠페인 ID, 목표 버전, 파일 크기·해시, 다운로드 URL
     */
    @GetMapping("/{vehicleId}/campaigns/{campaignId}/manifest")
    public ManifestResponse getManifest(@PathVariable String vehicleId, @PathVariable Long campaignId) {
        return ManifestResponse.from(manifestService.issue(vehicleId, campaignId));
    }
}
