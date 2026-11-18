package com.orbit.server.domain.artifact.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 펌웨어 서명의 대상. 파일 전체가 아니라 차종·버전·해시·크기를 담은 문자열에 서명한다. 해시가 파일 내용을 대표하고, 차종·버전을 넣어 다른
 * 차종·버전으로 바꿔치기하는 것을 막는다. (ORIGIN-POL-001)
 *
 * <p>관리자의 서명 절차와 차량의 검증이 같은 형식을 쓰므로 바꾸지 않는다. 형식을 바꿔야 하면 첫 줄을 {@code orbit-firmware-v2} 로 올린다.
 *
 * @param release 차종과 버전. 버전은 등록한 표기 그대로 들어가 {@code 1.0} 과 {@code 1.0.0} 은 서로 다른 서명이 된다
 * @param file 서버가 계산한 해시(소문자 16진수)와 크기
 */
public record FirmwareSignaturePayload(ArtifactRelease release, ArtifactFile file) {

    private static final String FORMAT = "orbit-firmware-v1";
    private static final String LINE_SEPARATOR = "\n";

    public FirmwareSignaturePayload {
        Objects.requireNonNull(release, "release");
        Objects.requireNonNull(file, "file");
    }

    /**
     * 다섯 줄({@code orbit-firmware-v1}, 차종, 버전, SHA-256, 크기)을 {@code \n} 으로 잇는다. 관리자가 셸의 {@code printf} 로 같은
     * 문자열을 만들므로 끝에 줄바꿈을 붙이지 않는다.
     *
     * @return 서명한 바이트열 (UTF-8)
     */
    public byte[] toBytes() {
        String payload = String.join(
                LINE_SEPARATOR,
                FORMAT,
                release.model(),
                release.version(),
                file.sha256(),
                Long.toString(file.sizeBytes()));
        return payload.getBytes(StandardCharsets.UTF_8);
    }
}
