-- [Phase2] Vehicle State: 매니페스트의 Edge 선택에 쓰는 차량 위치와 배정 Edge
-- 위치는 체크인이 보낸 마지막 GPS 좌표다. GPS 미수신이면 차량이 보내지 않으므로 처음부터 NULL 을 허용한다.
-- 위도·경도·갱신 시각은 한 번에 쓰므로 셋이 함께 있거나 함께 없어야 한다. 하나만 있으면 거리 계산과 최신 여부 판단이 어긋난다.
-- Edge 는 DB 가 아니라 설정(orbit.cdn.edges)에만 있으므로 assigned_edge_id 는 FK 없이 Edge id 문자열을 저장한다.
-- 설정에서 Edge 가 빠져도 과거 배정 기록은 남긴다.
ALTER TABLE vehicle_state
    ADD COLUMN latitude            DECIMAL(9, 6),
    ADD COLUMN longitude           DECIMAL(9, 6),
    ADD COLUMN location_updated_at DATETIME(3),
    ADD COLUMN assigned_edge_id    VARCHAR(50),
    ADD CONSTRAINT ck_vehicle_state_latitude CHECK (latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT ck_vehicle_state_longitude CHECK (longitude BETWEEN -180 AND 180),
    ADD CONSTRAINT ck_vehicle_state_location CHECK (
        (latitude IS NULL) = (longitude IS NULL)
        AND (latitude IS NULL) = (location_updated_at IS NULL)
    );
