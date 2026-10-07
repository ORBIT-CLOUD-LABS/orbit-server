-- Artifact: OTA로 배포하는 소프트웨어 패키지
CREATE TABLE artifact (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    version     VARCHAR(50)  NOT NULL,
    sha256      CHAR(64)     NOT NULL,
    size_bytes  BIGINT       NOT NULL,
    storage_uri VARCHAR(500) NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_artifact_name_version UNIQUE (name, version),
    CONSTRAINT ck_artifact_sha256 CHECK (REGEXP_LIKE(sha256, '^[0-9a-f]{64}$')),
    CONSTRAINT ck_artifact_size CHECK (size_bytes > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Campaign: 아티팩트를 배포하는 캠페인
CREATE TABLE campaign (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    artifact_id BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    started_at  DATETIME(6),
    ended_at    DATETIME(6),
    INDEX idx_campaign_artifact_id (artifact_id),
    INDEX idx_campaign_status (status),
    CONSTRAINT fk_campaign_artifact FOREIGN KEY (artifact_id) REFERENCES artifact (id),
    CONSTRAINT ck_campaign_status CHECK (status IN ('DRAFT', 'RUNNING', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_campaign_period CHECK (ended_at IS NULL OR started_at IS NULL OR ended_at >= started_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Campaign 대상 조건: field_name = expected_value 형태의 일치 조건
CREATE TABLE campaign_target_condition (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id    BIGINT       NOT NULL,
    field_name     VARCHAR(50)  NOT NULL,
    expected_value VARCHAR(200) NOT NULL,
    CONSTRAINT uk_campaign_target_condition UNIQUE (campaign_id, field_name, expected_value),
    CONSTRAINT fk_campaign_target_condition_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Vehicle: 차량 기본 정보
CREATE TABLE vehicle (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    vin        CHAR(17)     NOT NULL,
    model      VARCHAR(50)  NOT NULL,
    region     VARCHAR(50),
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_vehicle_vin UNIQUE (vin),
    CONSTRAINT ck_vehicle_vin CHECK (REGEXP_LIKE(vin, '^[A-HJ-NPR-Z0-9]{17}$'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Vehicle Credential: 차량 인증 토큰 (원문이 아닌 해시만 저장)
-- 유효(ACTIVE) 토큰 단일성: MySQL은 부분 유니크 인덱스가 없으므로
-- ACTIVE일 때만 값을 갖는 generated column에 UNIQUE를 건다 (UNIQUE는 NULL 중복 허용)
CREATE TABLE vehicle_credential (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id          BIGINT      NOT NULL,
    token_hash          CHAR(64)    NOT NULL,
    status              VARCHAR(20) NOT NULL,
    issued_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revoked_at          DATETIME(6),
    active_token_hash   CHAR(64)    AS (CASE WHEN status = 'ACTIVE' THEN token_hash END) VIRTUAL,
    active_vehicle_id   BIGINT      AS (CASE WHEN status = 'ACTIVE' THEN vehicle_id END) VIRTUAL,
    UNIQUE KEY uk_vehicle_credential_active_token (active_token_hash),
    UNIQUE KEY uk_vehicle_credential_active_vehicle (active_vehicle_id),
    INDEX idx_vehicle_credential_vehicle_id (vehicle_id),
    CONSTRAINT fk_vehicle_credential_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id) ON DELETE CASCADE,
    CONSTRAINT ck_vehicle_credential_token_hash CHECK (REGEXP_LIKE(token_hash, '^[0-9a-f]{64}$')),
    CONSTRAINT ck_vehicle_credential_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_vehicle_credential_revoked CHECK ((status = 'REVOKED') = (revoked_at IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Vehicle State: 차량의 현재 소프트웨어 상태
CREATE TABLE vehicle_state (
    vehicle_id      BIGINT PRIMARY KEY,
    current_version VARCHAR(50),
    last_seen_at    DATETIME(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_vehicle_state_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 차량별 캠페인 상태
CREATE TABLE vehicle_campaign_status (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id  BIGINT      NOT NULL,
    campaign_id BIGINT      NOT NULL,
    status      VARCHAR(20) NOT NULL,
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_vehicle_campaign_status UNIQUE (vehicle_id, campaign_id),
    INDEX idx_vehicle_campaign_status_campaign (campaign_id, status),
    CONSTRAINT fk_vehicle_campaign_status_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id) ON DELETE CASCADE,
    CONSTRAINT fk_vehicle_campaign_status_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE,
    CONSTRAINT ck_vehicle_campaign_status_status CHECK (status IN ('TARGETED', 'IN_PROGRESS', 'SUCCEEDED', 'FAILED', 'SKIPPED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 업데이트 시도
CREATE TABLE update_attempt (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id  BIGINT      NOT NULL,
    campaign_id BIGINT      NOT NULL,
    attempt_no  INT         NOT NULL,
    status      VARCHAR(20) NOT NULL,
    error_code  VARCHAR(50),
    started_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    finished_at DATETIME(6),
    CONSTRAINT uk_update_attempt_no UNIQUE (vehicle_id, campaign_id, attempt_no),
    CONSTRAINT fk_update_attempt_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id) ON DELETE CASCADE,
    CONSTRAINT fk_update_attempt_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE,
    CONSTRAINT ck_update_attempt_no CHECK (attempt_no > 0),
    CONSTRAINT ck_update_attempt_status CHECK (status IN ('IN_PROGRESS', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_update_attempt_finished CHECK ((status = 'IN_PROGRESS') = (finished_at IS NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 업데이트 이력 (상태 변경 이벤트 로그)
CREATE TABLE update_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id  BIGINT       NOT NULL,
    campaign_id BIGINT       NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    from_status VARCHAR(20),
    to_status   VARCHAR(20),
    detail      VARCHAR(500),
    occurred_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_update_history_vehicle_occurred (vehicle_id, occurred_at),
    INDEX idx_update_history_campaign (campaign_id),
    CONSTRAINT fk_update_history_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id) ON DELETE CASCADE,
    CONSTRAINT fk_update_history_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
