package com.orbit.server.domain.update.entity;

/**
 * 차량이 보고하는 업데이트 실패 사유. 차량 측 검증·설치 단계와 일대일로 대응한다.
 *
 * <p>DB 검사 제약(ck_update_result_failure_reason)과 같은 값만 둔다.
 */
public enum FailureReason {
    /** 모든 Edge(downloadUrl + fallbackUrls) 실패, 연결 끊김, 크기 불일치, 재시도 초과. */
    DOWNLOAD_FAILED,
    /** 받은 파일의 SHA-256 이 매니페스트와 다름. */
    HASH_MISMATCH,
    /** [Phase2] 펌웨어 서명 검증 실패, 또는 차량이 모르는 서명 키 버전(keyId). */
    SIGNATURE_INVALID,
    /** [Phase2] 목표 버전이 현재 버전보다 낮거나 읽을 수 없어 설치를 거부함. */
    DOWNGRADE_REJECTED,
    /** 설치 중 실패. */
    INSTALL_FAILED
}
