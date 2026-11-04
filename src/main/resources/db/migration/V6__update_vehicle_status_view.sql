-- [Phase2] Vehicle Status: 대시보드 지도에 차량 위치와 Edge 를 그리기 위한 컬럼 추가
-- V2 와 같은 이유로 이미 적용된 V2 는 고치지 않고 CREATE OR REPLACE VIEW 로 바꾼다.
-- 조인과 최근 결과 선택(상관 서브쿼리)은 V2 그대로 두어, ALGORITHM = MERGE 로 SSE 의 updated_at 조건이 계속 인덱스를 타게 한다.
--
-- assigned_edge_id   : 매니페스트가 마지막으로 배정한 Edge (vehicle_state)
-- downloaded_edge_id : 최근 업데이트 결과에서 차량이 실제로 파일을 받은 Edge (update_result.edge_id)
-- 위치는 체크인마다 vehicle_state 에 쓰므로 updated_at 이 바뀌어 SSE 변경분에 잡힌다.
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
       r.edge_id AS downloaded_edge_id
FROM vehicle v
    JOIN vehicle_state s ON s.vehicle_id = v.id
    LEFT JOIN update_result r ON r.id = (
        SELECT latest.id
        FROM update_result latest
        WHERE latest.vehicle_id = v.id
        ORDER BY latest.reported_at DESC, latest.id DESC
        LIMIT 1
    );
