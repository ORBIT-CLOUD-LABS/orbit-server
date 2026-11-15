package com.orbit.server.global.external.cdn;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 다운로드 URL 서명 설정을 읽고, 그 비밀 값으로 서명 계산기를 만든다. Edge 목록 설정({@link CdnConfig})과 따로 두어 각 설정을 독립적으로 검증한다. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UrlSigningProperties.class)
class UrlSigningConfig {

    @Bean
    SecureLinkSigner secureLinkSigner(UrlSigningProperties properties) {
        return new SecureLinkSigner(properties.secret());
    }
}
