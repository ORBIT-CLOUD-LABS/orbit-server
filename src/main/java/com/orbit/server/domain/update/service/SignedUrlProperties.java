package com.orbit.server.domain.update.service;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 다운로드 URL 서명의 유효 기간 설정. 재시도와 예비 Edge 까지 포함해 다운로드가 끝날 시간보다 길어야 하고, URL 이 유출되면 만료 전까지 누구나
 * 받을 수 있으므로 길게 잡지 않는다. 배포 환경마다 조정할 수 있도록 설정으로 둔다. (DOWNLOAD-POL-001)
 *
 * <p>같은 {@code orbit.cdn} 아래의 서명 비밀 값은 외부 연동 구현의 일이라 {@code global/external/cdn} 이 읽는다. 유효 기간은 매니페스트
 * 발급 규칙이라 이 컨텍스트가 읽는다.
 *
 * <p>Edge 는 만료를 초 단위로 비교하므로 1초보다 짧으면 발급하자마자 만료된다. 그런 값이면 앱을 띄우지 않는다.
 *
 * @param signedUrlTtl 발급 시각부터 URL 이 유효한 시간. 환경 변수 {@code CDN_SIGNED_URL_TTL} 로 바꾼다
 */
@Validated
@ConfigurationProperties("orbit.cdn")
public record SignedUrlProperties(
        @NotNull @DurationMin(seconds = 1) Duration signedUrlTtl) {

    /**
     * 지금 발급하는 URL 의 만료 시각을 정한다.
     *
     * @param issuedAt 매니페스트를 발급하는 시각
     * @return 만료 시각. URL 의 {@code expires}(Unix 초)와 응답의 만료 시각이 같은 시각을 가리키도록 초 아래는 버린다
     */
    public Instant expiresAt(Instant issuedAt) {
        return issuedAt.plus(signedUrlTtl).truncatedTo(ChronoUnit.SECONDS);
    }
}
