package com.orbit.server.global.external.cdn;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 차량이 업데이트 파일을 받는 CDN 설정. 배포 환경마다 주소가 달라 환경 변수로 주입한다. (DOWNLOAD-REQ-001)
 *
 * <p>주소가 없으면 매니페스트마다 잘못된 URL 을 내려주게 되므로 값이 없으면 앱을 띄우지 않는다.
 *
 * @param baseUrl CDN 주소. 환경 변수 {@code CDN_BASE_URL} 로 주입한다. 예: {@code http://cdn.orbit.internal}
 */
@Validated
@ConfigurationProperties("orbit.cdn")
record CdnProperties(@NotNull URI baseUrl) {}
