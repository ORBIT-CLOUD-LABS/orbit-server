-- Artifact: Origin 에 등록한 업데이트 파일 메타데이터
-- 등록 후 수정하지 않으므로 updated_at 을 두지 않는다. 버전마다 새 Origin 경로를 쓴다.
CREATE TABLE artifact (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    model       VARCHAR(50)  NOT NULL,
    version     VARCHAR(50)  NOT NULL,
    sha256      CHAR(64)     NOT NULL,
    size_bytes  BIGINT       NOT NULL,
    signature   VARCHAR(128) NOT NULL,
    path        VARCHAR(500) NOT NULL,
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_artifact_model_version UNIQUE (model, version),
    CONSTRAINT uk_artifact_path UNIQUE (path),
    CONSTRAINT ck_artifact_sha256 CHECK (REGEXP_LIKE(sha256, '^[0-9a-f]{64}$')),
    CONSTRAINT ck_artifact_size CHECK (size_bytes > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Campaign: 아티팩트를 대상 차량에 배포하는 캠페인
-- 목표 버전은 artifact.version 과 같아야 하므로 중복 저장하지 않는다.
-- 1:1 대상 조건(차종, 현재 버전 범위)은 이 테이블에 두고, 1:N 조건은 별도 테이블에 둔다.
CREATE TABLE campaign (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    artifact_id         BIGINT       NOT NULL,
    name                VARCHAR(100) NOT NULL,
    model               VARCHAR(50)  NOT NULL,
    current_version_min VARCHAR(50),
    current_version_max VARCHAR(50),
    start_at            DATETIME(3)  NOT NULL,
    end_at              DATETIME(3)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    created_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    -- 체크인 대상 판정에서 활성 캠페인을 차종으로 좁혀 가져온다
    INDEX idx_campaign_status_model (status, model),
    INDEX idx_campaign_artifact_id (artifact_id),
    CONSTRAINT fk_campaign_artifact FOREIGN KEY (artifact_id) REFERENCES artifact (id),
    CONSTRAINT ck_campaign_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_campaign_period CHECK (start_at < end_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Campaign 대상 HW 버전. 행이 없으면 모든 HW 버전이 대상이다.
CREATE TABLE campaign_target_hw_version (
    campaign_id BIGINT      NOT NULL,
    hw_version  VARCHAR(50) NOT NULL,
    PRIMARY KEY (campaign_id, hw_version),
    CONSTRAINT fk_campaign_target_hw_version_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Campaign 대상 지역. 행이 없으면 모든 지역이 대상이다.
CREATE TABLE campaign_target_region (
    campaign_id BIGINT      NOT NULL,
    region      VARCHAR(50) NOT NULL,
    PRIMARY KEY (campaign_id, region),
    CONSTRAINT fk_campaign_target_region_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Vehicle: 등록 시 갱신하는 차량 정보
-- external_id 는 차량이 보내는 vehicleId 로, API 경로에 쓰인다.
CREATE TABLE vehicle (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(64) NOT NULL,
    model       VARCHAR(50) NOT NULL,
    hw_version  VARCHAR(50) NOT NULL,
    region      VARCHAR(50) NOT NULL,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_vehicle_external_id UNIQUE (external_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Vehicle Credential: 차량 인증 토큰 (원문이 아닌 해시만 저장)
-- 유효(ACTIVE) 토큰 단일성: MySQL은 부분 유니크 인덱스가 없으므로
-- ACTIVE일 때만 값을 갖는 generated column에 UNIQUE를 건다 (UNIQUE는 NULL 중복 허용)
CREATE TABLE vehicle_credential (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id          BIGINT      NOT NULL,
    token_hash          CHAR(64)    NOT NULL,
    status              VARCHAR(20) NOT NULL,
    issued_at           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    revoked_at          DATETIME(3),
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

-- Vehicle State: 체크인마다 덮어쓰는 차량의 현재 상태
-- 쓰기가 잦아 vehicle 과 분리한다. 대시보드 스냅샷과 SSE 변경분 조회(updated_at 기준)의 출처다.
CREATE TABLE vehicle_state (
    vehicle_id      BIGINT PRIMARY KEY,
    current_version VARCHAR(50) NOT NULL,
    last_seen_at    DATETIME(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_vehicle_state_updated_at (updated_at),
    CONSTRAINT fk_vehicle_state_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Update Result: 차량이 체크인으로 보고한 캠페인별 업데이트 결과
-- 같은 결과가 다시 보고돼도 (vehicle_id, campaign_id) 기준으로 덮어써서 멱등하게 처리한다.
CREATE TABLE update_result (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id     BIGINT       NOT NULL,
    campaign_id    BIGINT       NOT NULL,
    result         VARCHAR(20)  NOT NULL,
    failure_reason VARCHAR(30),
    failure_detail VARCHAR(500),
    finished_at    DATETIME(3)  NOT NULL,
    reported_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_update_result_vehicle_campaign UNIQUE (vehicle_id, campaign_id),
    -- 캠페인 진행 현황(결과·실패 사유별 집계) 조회용
    INDEX idx_update_result_campaign_result (campaign_id, result),
    CONSTRAINT fk_update_result_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id) ON DELETE CASCADE,
    CONSTRAINT fk_update_result_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE,
    CONSTRAINT ck_update_result_result CHECK (result IN ('SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_update_result_failure_reason CHECK (
        failure_reason IS NULL
        OR failure_reason IN ('DOWNLOAD_FAILED', 'HASH_MISMATCH', 'SIGNATURE_INVALID', 'INSTALL_FAILED')
    ),
    CONSTRAINT ck_update_result_failure CHECK ((result = 'FAILED') = (failure_reason IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
