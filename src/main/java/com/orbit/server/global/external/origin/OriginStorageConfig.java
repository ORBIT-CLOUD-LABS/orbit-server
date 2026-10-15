package com.orbit.server.global.external.origin;

import com.orbit.server.domain.artifact.service.OriginStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Origin 원본 저장소를 마운트 경로에 쓰는 구현으로 연결한다. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OriginProperties.class)
class OriginStorageConfig {

    @Bean
    OriginStorage originStorage(OriginProperties properties) {
        return new FileSystemOriginStorage(properties.rootPath());
    }
}
