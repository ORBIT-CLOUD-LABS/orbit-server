-- Vehicle Status: 대시보드 스냅샷과 SSE 가 읽는 차량별 현재 상태와 최근 업데이트 결과
-- 대시보드는 읽기만 하므로 별도 테이블로 동기화하지 않고 vehicle, vehicle_state, update_result 를 조인해 보여 준다.
-- 이 세 테이블의 컬럼을 바꾸면 MySQL 이 막지 않고 뷰 조회 때 오류가 나므로 이 뷰도 함께 고친다.
-- 이미 적용된 이 파일은 고치지 않고 새 마이그레이션에서 CREATE OR REPLACE VIEW 로 바꾼다.
--
-- 최근 결과는 보고 시각(reported_at)이 늦은 것, 같으면 나중에 기록한(id 가 큰) 것이다.
-- 윈도 함수를 쓰면 뷰가 TEMPTABLE 로 처리돼 SSE 의 updated_at 조건이 뷰 안으로 들어가지 못하고 매번 전체를 만들므로,
-- 바깥 조건이 합쳐지는 MERGE 로 처리되도록 상관 서브쿼리로 붙인다.
CREATE ALGORITHM = MERGE VIEW vehicle_status AS
SELECT v.external_id,
       v.model,
       s.current_version,
       s.last_seen_at,
       s.updated_at,
       r.campaign_id,
       r.result,
       r.failure_reason
FROM vehicle v
    JOIN vehicle_state s ON s.vehicle_id = v.id
    LEFT JOIN update_result r ON r.id = (
        SELECT latest.id
        FROM update_result latest
        WHERE latest.vehicle_id = v.id
        ORDER BY latest.reported_at DESC, latest.id DESC
        LIMIT 1
    );
