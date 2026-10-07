package com.orbit.server.domain.update.model;

/** 차량이 보고하는 업데이트 실패 사유. 차량 측 검증·설치 단계와 일대일로 대응한다. */
public enum FailureReason {
    /** CDN 오류, 연결 끊김, 크기 불일치, 재시도 초과. */
    DOWNLOAD_FAILED,
    /** 받은 파일의 SHA-256 이 매니페스트와 다름. */
    HASH_MISMATCH,
    /** Ed25519 서명 검증 실패. */
    SIGNATURE_INVALID,
    /** 설치 중 실패. */
    INSTALL_FAILED
}
