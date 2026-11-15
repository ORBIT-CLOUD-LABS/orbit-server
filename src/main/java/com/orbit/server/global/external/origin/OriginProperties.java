package com.orbit.server.global.external.origin;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Origin 저장소(AWS S3 또는 S3 호환 서버) 접속 설정. 접속 정보와 비밀 값은 환경 변수로 주입한다.
 *
 * @param endpoint S3 호환 서버 주소. 예: SeaweedFS {@code http://host:8333}. 비우면 AWS S3 기본 엔드포인트로 접속한다
 * @param region 서명과 AWS 엔드포인트 결정에 쓰는 리전. 예: {@code ap-northeast-2}
 * @param bucket 펌웨어를 둘 버킷. 인프라에서 미리 만들어 둔다
 * @param accessKey 접근 키
 * @param secretKey 비밀 키
 */
@Validated
@ConfigurationProperties("orbit.origin")
record OriginProperties(
        URI endpoint,
        @NotBlank String region,
        @NotBlank String bucket,
        @NotBlank String accessKey,
        @NotBlank String secretKey) {

    /**
     * AWS 기본 엔드포인트 대신 접속할 S3 호환 서버 주소.
     *
     * @return 설정한 주소. 설정하지 않았으면 빈 값
     */
    Optional<URI> customEndpoint() {
        return Optional.ofNullable(endpoint);
    }
}
