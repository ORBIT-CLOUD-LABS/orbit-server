-- Vehicle Event: 차량별 업데이트 시도 이력
-- update_result 는 (vehicle_id, campaign_id) 당 한 행을 덮어쓰므로, 실패 후 재시도로 성공하면 실패 기록이 사라진다.
-- 어떤 버전이 언제 실패·성공했는지 남기려고 보고된 결과를 시도마다 한 행씩 쌓는다. 이 테이블을 추가한 시점부터만 쌓인다.
-- 버전은 campaign → artifact.version 으로 읽는다. 캠페인의 artifact 는 바뀌지 않으므로 중복 저장하지 않는다.
--
-- 차량은 응답을 못 받으면 같은 결과를 다시 보낸다. 같은 시도는 finished_at 이 같으므로
-- UNIQUE (vehicle_id, campaign_id, finished_at) 로 재전송을 걸러 내고, 재시도는 finished_at 이 달라 따로 남는다.
-- result·실패 사유·edge_id 규칙은 update_result 와 같다.
CREATE TABLE vehicle_event (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id     BIGINT       NOT NULL,
    campaign_id    BIGINT       NOT NULL,
    result         VARCHAR(20)  NOT NULL,
    failure_reason VARCHAR(30),
    failure_detail VARCHAR(500),
    edge_id        VARCHAR(50),
    finished_at    DATETIME(3)  NOT NULL,
    recorded_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_vehicle_event_attempt UNIQUE (vehicle_id, campaign_id, finished_at),
    -- 차량별 이력을 완료 시각 역순으로 조회한다
    INDEX idx_vehicle_event_vehicle_finished (vehicle_id, finished_at),
    INDEX idx_vehicle_event_campaign_id (campaign_id),
    CONSTRAINT fk_vehicle_event_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id) ON DELETE CASCADE,
    CONSTRAINT fk_vehicle_event_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE,
    CONSTRAINT ck_vehicle_event_result CHECK (result IN ('SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_vehicle_event_failure_reason CHECK (
        failure_reason IS NULL
        OR failure_reason IN (
            'DOWNLOAD_FAILED', 'HASH_MISMATCH', 'INSTALL_FAILED', 'SIGNATURE_INVALID', 'DOWNGRADE_REJECTED'
        )
    ),
    CONSTRAINT ck_vehicle_event_failure CHECK ((result = 'FAILED') = (failure_reason IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
