-- Vehicle Status: 대시보드 차량 상세에 보여 줄 컬럼 추가 (#23)
-- 차량 상세는 최근 업데이트 결과를 스냅샷·SSE 와 같은 기준으로 골라야 하므로, 따로 조회하지 않고 이 뷰에 컬럼을 더한다.
-- V6 와 같은 이유로 이미 적용된 파일은 고치지 않고 CREATE OR REPLACE VIEW 로 바꾼다.
-- 조인과 최근 결과 선택(상관 서브쿼리)은 V6 그대로 두어, ALGORITHM = MERGE 로 SSE 의 updated_at 조건이 계속 인덱스를 타게 한다.
--
-- hw_version, region : 차량 등록 때 받은 HW 버전과 판매·등록 지역 (vehicle)
-- failure_detail     : 최근 업데이트 결과의 실패 상세 (update_result)
-- finished_at        : 최근 업데이트를 차량이 끝낸 시각 (update_result)
CREATE OR REPLACE ALGORITHM = MERGE VIEW vehicle_status AS
SELECT v.external_id,
       v.model,
       s.current_version,
       s.last_seen_at,
       s.updated_at,
       r.campaign_id,
       r.result,
       r.failure_reason,
       s.latitude,
       s.longitude,
       s.location_updated_at,
       s.assigned_edge_id,
       r.edge_id AS downloaded_edge_id,
       v.hw_version,
       v.region,
       r.failure_detail,
       r.finished_at
FROM vehicle v
    JOIN vehicle_state s ON s.vehicle_id = v.id
    LEFT JOIN update_result r ON r.id = (
        SELECT latest.id
        FROM update_result latest
        WHERE latest.vehicle_id = v.id
        ORDER BY latest.reported_at DESC, latest.id DESC
        LIMIT 1
    );
