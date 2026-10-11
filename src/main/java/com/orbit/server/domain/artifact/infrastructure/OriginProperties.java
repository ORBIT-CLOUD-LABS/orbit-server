package com.orbit.server.domain.artifact.infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Origin File Server(S3 호환) 접속 설정. 접속 정보와 비밀 값은 환경 변수로 주입한다.
 *
 * @param endpoint S3 API 주소. 예: SeaweedFS {@code http://host:8333}
 * @param region 서명에 쓰는 리전. S3 호환 서버는 값을 검사하지 않지만 SDK 가 요구한다
 * @param bucket 펌웨어를 둘 버킷. 인프라에서 미리 만들어 둔다
 * @param accessKey 접근 키
 * @param secretKey 비밀 키
 */
@Validated
@ConfigurationProperties("orbit.origin")
record OriginProperties(
        @NotNull URI endpoint,
        @NotBlank String region,
        @NotBlank String bucket,
        @NotBlank String accessKey,
        @NotBlank String secretKey) {}
