package com.orbit.server.global.external.origin;

import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Origin 원본 저장소 설정. 환경마다 마운트 위치가 다르므로 환경 변수로 주입한다.
 *
 * @param rootPath 원본 저장소(NFS)를 마운트한 경로. Origin Nginx 가 제공하는 루트와 같은 저장소여야 한다
 */
@Validated
@ConfigurationProperties("orbit.origin")
record OriginProperties(@NotNull Path rootPath) {}
