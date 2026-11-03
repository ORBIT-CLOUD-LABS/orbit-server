-- [Phase2] Artifact: 관리자가 Vault 로 만든 펌웨어 서명(Ed25519)과 서명 키 버전
-- 서버는 등록 때 서명을 검증만 하고, 매니페스트로 그대로 내려줘 차량이 설치 전에 다시 검증한다.
--
-- 최종적으로는 NOT NULL 이지만 지금은 NULL 을 허용한다.
-- 등록 API 가 서명을 받기 전(#89)에 NOT NULL 로 만들면 Origin 파일 등록이 모두 실패하기 때문이다.
-- #89 에서 등록이 서명을 저장하게 한 뒤 NOT NULL 로 바꾼다.
--
-- signature : Ed25519 서명 64바이트의 base64. 패딩을 포함해 항상 88자다.
-- key_id    : Vault Transit 키 버전 (v1, v2, ...). 차량은 이 값으로 내장 공개 키를 고른다.
-- 서명과 키 버전은 한 쌍이라 둘이 함께 있거나 함께 없어야 한다.
-- 테이블 정렬 규칙이 대소문자를 구분하지 않으므로 정규식은 'c'(대소문자 구분)로 검사한다.
ALTER TABLE artifact
    ADD COLUMN signature CHAR(88),
    ADD COLUMN key_id    VARCHAR(20),
    ADD CONSTRAINT ck_artifact_signature CHECK (REGEXP_LIKE(signature, '^[A-Za-z0-9+/]{86}==$', 'c')),
    ADD CONSTRAINT ck_artifact_key_id CHECK (REGEXP_LIKE(key_id, '^v[1-9][0-9]*$', 'c')),
    ADD CONSTRAINT ck_artifact_signature_key CHECK ((signature IS NULL) = (key_id IS NULL));
