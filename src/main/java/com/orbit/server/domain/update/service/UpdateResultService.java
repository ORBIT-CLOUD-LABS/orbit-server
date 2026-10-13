package com.orbit.server.domain.update.service;

import com.orbit.server.domain.campaign.service.CampaignQueryService;
import com.orbit.server.domain.update.entity.UpdateOutcome;
import com.orbit.server.domain.update.repository.UpdateResultRepository;
import com.orbit.server.global.error.InvalidValueException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차량이 체크인으로 보고한 업데이트 결과를 기록한다. 같은 결과가 다시 와도 결과는 같다. (UPDATE-REQ-001 / UPDATE-POL-001)
 *
 * <p>다른 컨텍스트인 캠페인은 {@link CampaignQueryService} 로만 확인한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateResultService {

    private final CampaignQueryService campaignQueryService;
    private final UpdateResultRepository updateResultRepository;

    /**
     * 보고 검증 → 캠페인 확인 → 기록 순서로 처리한다.
     *
     * <p>형식이 틀린 보고와 없는 캠페인의 결과는 기록하지 않고 버린다. 차량은 체크인이 성공할 때까지 같은 결과를 다시 보내므로, 오류로
     * 응답하면 그 차량의 체크인이 계속 실패한다.
     *
     * @param vehicleId 결과를 보고한 차량 ID
     * @param report 차량이 보낸 업데이트 결과
     */
    @Transactional
    public void record(Long vehicleId, ReportUpdateResultCommand report) {
        Optional<UpdateOutcome> outcome = validate(vehicleId, report);
        if (outcome.isEmpty() || !campaignExists(vehicleId, report)) {
            return;
        }
        updateResultRepository.record(vehicleId, report.campaignId(), outcome.get());
    }

    // 형식이 틀린 보고는 차량 쪽 버그이므로 ERROR 로 남겨 드러낸다. 상세는 길이를 검증하지 못했으므로 로그에 남기지 않는다
    private Optional<UpdateOutcome> validate(Long vehicleId, ReportUpdateResultCommand report) {
        try {
            return Optional.of(report.toOutcome());
        } catch (InvalidValueException e) {
            log.error(
                    "형식이 틀린 업데이트 결과를 버립니다: vehicleId={}, campaignId={}, reason={}",
                    vehicleId,
                    report.campaignId(),
                    e.getMessage(),
                    e);
            return Optional.empty();
        }
    }

    private boolean campaignExists(Long vehicleId, ReportUpdateResultCommand report) {
        if (campaignQueryService.exists(report.campaignId())) {
            return true;
        }
        log.warn(
                "없는 캠페인의 업데이트 결과를 버립니다: vehicleId={}, campaignId={}, result={}",
                vehicleId,
                report.campaignId(),
                report.result());
        return false;
    }
}
