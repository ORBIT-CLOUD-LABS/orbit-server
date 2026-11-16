package com.orbit.server.domain.artifact.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 펌웨어 서명 공개 키 설정을 읽어 검증에 쓸 공개 키 목록을 만든다. {@code global} 은 {@code domain} 타입에 의존할 수 없으므로 설정을 쓰는
 * 컨텍스트에 둔다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SigningProperties.class)
class SigningConfig {

    // 공개 키가 잘못되면 첫 등록 요청이 아니라 기동 때 실패하도록 빈을 만들 때 검증한다
    @Bean
    FirmwarePublicKeys firmwarePublicKeys(SigningProperties properties) {
        return properties.toPublicKeys();
    }
}
