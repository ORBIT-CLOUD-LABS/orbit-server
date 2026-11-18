package com.orbit.server.domain.artifact.entity;

import java.util.Base64;

/**
 * 서명 검증을 거치지 않고 Artifact 를 바로 저장하는 테스트가 쓰는 서명. 형식만 맞고 어떤 공개 키로도 검증되지 않는다.
 *
 * <p>서명 검증 자체는 {@code FirmwareSignatureVerifierTest} 가 실행 중에 만든 키 쌍으로 확인한다.
 */
public final class ArtifactSignatureFixture {

    /** 64바이트 0 의 base64 와 키 버전 v1. */
    public static final ArtifactSignature SIGNATURE =
            new ArtifactSignature(Base64.getEncoder().encodeToString(new byte[64]), "v1");

    private ArtifactSignatureFixture() {}
}
