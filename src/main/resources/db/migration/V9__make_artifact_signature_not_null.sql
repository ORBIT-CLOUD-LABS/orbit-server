-- [Phase2] Artifact: 펌웨어 서명(signature)과 서명 키 버전(key_id)을 필수로 바꾼다 (#89)
-- 등록 API 가 서명을 검증해 저장하게 됐으므로, 서명 없는 파일을 허용하는 예외 경로를 DB 에도 남기지 않는다.
--
-- [배포 전 확인] 서명 없는 artifact 행이 남아 있으면 이 마이그레이션은 실패하고 서버가 뜨지 않는다.
-- 서명 없는 파일이 남은 상태를 조용히 넘기지 않고 드러내기 위해, 서명 없는 행을 지우거나 임의 값으로 채우지 않는다.
-- 그런 환경은 배포 전에 운영자가 아래 순서로 정리한다.
--   1. 서명 없는 artifact 확인: SELECT id, model, version FROM artifact WHERE signature IS NULL;
--   2. 그 artifact 를 참조하는 캠페인을 지운다. 캠페인의 대상 조건·결과·이벤트(campaign_target_*, update_result, vehicle_event)는
--      ON DELETE CASCADE 로 함께 지워진다.
--   3. 서명 없는 artifact 를 지운다.
--   4. 파일을 Vault 로 서명해 다시 등록하고, 필요하면 캠페인을 다시 만든다.
-- 마이그레이션이 행을 지우지 않는 이유: campaign 이 artifact 를 FK 로 참조해 캠페인과 그 결과까지 지워야 하기 때문이다.
--
-- 타입은 V5 와 같고 NULL 허용만 바꾼다. V5 의 CHECK 제약은 그대로 둔다.
-- ck_artifact_signature_key(둘이 함께 있거나 함께 없음)는 이제 항상 참이지만, 지워도 얻는 것이 없어 남긴다.
ALTER TABLE artifact
    MODIFY COLUMN signature CHAR(88)    NOT NULL,
    MODIFY COLUMN key_id    VARCHAR(20) NOT NULL;
