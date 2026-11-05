package com.orbit.server.domain.monitoring.service;

/**
 * 진행 현황에서 실패 수를 나눠 보여 주는 실패 사유. 보고된 적 없는 사유도 0 으로 보여 주려면 사유 목록이 필요하다.
 *
 * <p>update 컨텍스트의 entity 인 {@code FailureReason} 을 참조하지 않도록 같은 값을 따로 둔다. 두 목록이 같은지는 테스트로 확인한다.
 */
public enum UpdateFailureReason {
    DOWNLOAD_FAILED,
    HASH_MISMATCH,
    SIGNATURE_INVALID,
    DOWNGRADE_REJECTED,
    INSTALL_FAILED
}
