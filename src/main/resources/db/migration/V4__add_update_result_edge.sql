-- [Phase2] Update Result: 차량이 실제로 파일을 받은 Edge 와 새 실패 사유
-- 매니페스트가 배정한 Edge(vehicle_state.assigned_edge_id)가 실패하면 차량은 fallbackUrls 의 다른 Edge 로 받는다.
-- 그래서 배정 Edge 와 별개로, 업데이트 결과 한 건마다 실제로 받은 Edge 를 남겨 Edge 별 성공·실패를 집계한다.
-- 다운로드 전에 실패했거나 Phase1 에서 기록한 결과에는 값이 없으므로 NULL 을 허용한다.
-- Edge 는 설정에만 있으므로 FK 없이 Edge id 문자열을 저장한다.
--
-- 차량이 펌웨어 서명을 검증하고 다운그레이드를 거부하게 되므로 실패 사유 두 가지를 허용한다.
--   SIGNATURE_INVALID  : 펌웨어 서명 검증 실패, 모르는 keyId
--   DOWNGRADE_REJECTED : 목표 버전이 현재 버전보다 낮거나 읽을 수 없음
ALTER TABLE update_result
    ADD COLUMN edge_id VARCHAR(50),
    DROP CHECK ck_update_result_failure_reason,
    ADD CONSTRAINT ck_update_result_failure_reason CHECK (
        failure_reason IS NULL
        OR failure_reason IN (
            'DOWNLOAD_FAILED', 'HASH_MISMATCH', 'INSTALL_FAILED', 'SIGNATURE_INVALID', 'DOWNGRADE_REJECTED'
        )
    );
