package com.orbit.server.domain.artifact.entity;

import com.orbit.server.domain.shared.DottedVersion;
import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 업데이트 파일이 어떤 차종의 어떤 버전인지 나타낸다. 차종과 버전 조합은 Artifact 마다 유일하다.
 *
 * @param model 차종. 공백 불가, 50자 이하, '/' 불가
 * @param version 소프트웨어 버전. 공백 불가, 50자 이하, 점으로 나눈 숫자 형식 (예: {@code 1.0.1})
 */
@Embeddable
public record ArtifactRelease(
        @Column(name = "model") String model,
        @Column(name = "version") String version) {

    private static final int MAX_LENGTH = 50;
    private static final String PATH_SEPARATOR = "/";
    private static final String ORIGIN_PATH_FORMAT = "/firmware/%s/%s/firmware.bin";
    private static final String VERSION_NAME = "버전";

    /**
     * @throws InvalidValueException 차종이나 버전이 비어 있거나 50자를 넘는 경우, 차종이 '/' 를 포함하는 경우, 버전이 점으로 나눈 숫자
     *     형식이 아닌 경우
     */
    // 버전은 숫자와 점만 허용하므로 '/' 검사를 따로 하지 않아도 Origin 경로가 겹치지 않는다
    public ArtifactRelease {
        requirePathSegment(Preconditions.requireText(model, MAX_LENGTH, "차종"), "차종");
        DottedVersion.of(Preconditions.requireText(version, MAX_LENGTH, VERSION_NAME), VERSION_NAME);
    }

    /**
     * 이 차종·버전의 파일을 둘 Origin 경로. 등록한 파일은 수정하지 않으므로 버전마다 새 경로를 쓴다. (ORIGIN-POL-001)
     *
     * @return {@code /firmware/{model}/{version}/firmware.bin}
     */
    public String originPath() {
        return ORIGIN_PATH_FORMAT.formatted(model, version);
    }

    /**
     * 등록된 다른 버전이 비교 규칙상 이 버전과 같은지 판정한다. 표기는 입력한 그대로 두되, {@code 1.0} 과 {@code 1.0.0} 처럼 대상
     * 판정에서 같은 버전으로 보는 파일이 한 차종에 둘 생기지 않게 등록 전 중복 확인에 쓴다. (VEHICLE-POL-001)
     *
     * @param registeredVersion 같은 차종으로 등록된 버전. 입력 단계에서 형식을 검증해 저장한 값
     * @return 숫자 자리로 비교해 같으면 true
     */
    public boolean isSameVersionAs(String registeredVersion) {
        return DottedVersion.of(version, VERSION_NAME).equals(DottedVersion.of(registeredVersion, VERSION_NAME));
    }

    // '/' 를 허용하면 다른 차종·버전 조합이 같은 경로를 만들어 기존 파일을 덮어쓸 수 있다
    private static void requirePathSegment(String value, String name) {
        if (value.contains(PATH_SEPARATOR)) {
            throw new InvalidValueException(name + " 은(는) '/' 를 포함할 수 없습니다.");
        }
    }
}
